/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package u212;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.core.common.ActivationsFilter;
import org.drools.core.common.InternalAgenda;
import org.drools.core.common.InternalWorkingMemory;
import org.drools.core.reteoo.TerminalNode;
import org.drools.core.spi.Activation;
import org.drools.model.DSL;
import org.drools.model.Model;
import org.drools.model.Rule;
import org.drools.model.Variable;
import org.drools.model.impl.ModelImpl;
import org.drools.modelcompiler.builder.KieBaseBuilder;
import org.drools.modelcompiler.domain.Person;
import org.drools.modelcompiler.domain.Result;

import org.drools.modelcompiler.u212.Persister;
import org.drools.modelcompiler.u212.PersisterImpl;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.drools.model.PatternDSL.pattern;
import static org.drools.model.PatternDSL.reactOn;
import static org.drools.model.PatternDSL.rule;
import static org.junit.Assert.*;

public class SnapshooterTest {
    private Logger logger = LoggerFactory.getLogger(SnapshooterTest.class);


    @Test
    public void testBeta() {

        List<Result> resultList = new ArrayList<>();

        Variable<Person> markV = DSL.declarationOf(Person.class);

        Rule rule = rule("beta")
                .build(
                        pattern(markV)
                                .expr("exprA",
                                      p -> p.getAge()>= 37,
                                      reactOn("name",
                                              "age")),
                        DSL.on(markV).execute((p1) -> {
                            Result result = new Result();
                            result.setValue(p1.getName());
                            resultList.add(result);
                            System.out.println(p1.getName());
                         })
                );

        Model model = new ModelImpl().addRule(rule);
        KieBase kieBase = KieBaseBuilder.createKieBaseFromModel(model);

        KieSession ksession = kieBase.newKieSession();
        Map collectedObjects = new LinkedHashMap();
        Set<PersisterImpl.MatchDTO> collectedFireMatches = new HashSet<>(1);

        Persister persister = new PersisterImpl(collectedObjects, collectedFireMatches, ksession);


        Person mark = new Person("Mark",
                                 37);
        Person edson = new Person("Edson",
                                  35);
        Person mario = new Person("Mario",
                                  40);

        FactHandle markFH =  persister.add(mark);
        FactHandle edsonFH = persister.add(edson);
        FactHandle marioFH = persister.add(mario);

        persister.fireAllRules(1);
        assertEquals(1, resultList.size());
        assertEquals("Mario", resultList.get(0).getValue());
        assertEquals(collectedObjects.size(), 3);

        PersisterImpl.MatchDTO marioMatchDTO =  PersisterImpl.MatchDTO.create(rule.getPackage(), rule.getName(), marioFH);
        Set<PersisterImpl.MatchDTO> expectedDTO = new HashSet<>();
        expectedDTO.add(marioMatchDTO);

System.out.println("+++++++++++++++++++++++++");
        KieSession newFreshKsession = kieBase.newKieSession();
        newFreshKsession.addEventListener(new DebugRuleRuntimeEventListener());
        ((InternalAgenda)newFreshKsession.getAgenda()).setActivationsFilter(new CancelMatches(collectedFireMatches));
        for(Object o : collectedObjects.values()){
            newFreshKsession.insert(o);
        }
        newFreshKsession.fireAllRules();

        assertEquals(2, resultList.size());
        assertEquals("Mark", resultList.get(1).getValue());


        /*
        result.setValue(null);
        persister.remove(marioFH);
        persister.fireAllRules();
        assertNull(result.getValue());

        mark.setAge(34);
        persister.update(markFH,
                        mark);

        persister.fireAllRules();
        assertEquals("Edson is older than Mark",
                     result.getValue());*/
    }

    public static class CancelMatches implements ActivationsFilter {

        private Set<PersisterImpl.MatchDTO> matchesDTO;

        public CancelMatches(Set<PersisterImpl.MatchDTO> matchesDTO) {
            this.matchesDTO = matchesDTO;
        }

        @Override
        public boolean accept(Activation activation,
                              InternalWorkingMemory internalWorkingMemory,
                              TerminalNode terminalNode) {
            if(activation.isRuleAgendaItem()){
               return true;
            }else {
                PersisterImpl.MatchDTO dto = PersisterImpl.MatchDTO.create(activation);
                boolean res  = matchesDTO.remove(dto);
                System.out.println("remove:"+dto +" rs:"+res);
                return !res;
            }
        }
    }

}

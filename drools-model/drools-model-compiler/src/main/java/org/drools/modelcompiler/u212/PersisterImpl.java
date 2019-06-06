package org.drools.modelcompiler.u212;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.drools.core.common.InternalFactHandle;
import org.drools.core.event.DefaultAgendaEventListener;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.api.runtime.rule.Match;

public class PersisterImpl implements Persister {

    private Map map;
    private KieSession kieSession;

    public PersisterImpl(Map map, Set<MatchDTO> activations,
                         KieSession kieSession) {
        this.map = map;
        this.kieSession = kieSession;
        AgendaEventListener listener = new DefaultAgendaEventListener(){
            @Override
            public void afterMatchFired(AfterMatchFiredEvent event) {
                activations.add(MatchDTO.create(event.getMatch()));
            }
        };

        kieSession.addEventListener(listener);
    }



    @Override
    public FactHandle add(Object o) {
        FactHandle result = kieSession.insert(o);
        map.put(result,o);
        return result;
    }

    @Override
    public void update(FactHandle factHandle, Object o) {
        map.put(factHandle, o);
        kieSession.update(factHandle, o);
    }

    @Override
    public void remove(FactHandle factHandle) {
        kieSession.delete(factHandle);
        map.remove(factHandle);
    }

    @Override
    public void fireAllRules() {
        kieSession.fireAllRules();
    }

    @Override
    public void fireAllRules(int fireLimit) {
        kieSession.fireAllRules(fireLimit);
    }



    public static class MatchDTO {
        private final String pkgName;
        private final String ruleName;
        private final List<Integer> tuple;

        public MatchDTO(String pkgName,
                        String ruleName,
                        List<Integer> tuple) {
            this.pkgName = pkgName;
            this.ruleName = ruleName;
            this.tuple = tuple;
        }

        public String getPkgName() {
            return pkgName;
        }

        public String getRuleName() {
            return ruleName;
        }

        public List<Integer> getTuple() {
            return tuple;
        }

        public static MatchDTO create(Match match){

            List<Integer> list = new ArrayList<>();
            for(FactHandle fact : match.getFactHandles()){
                list.add(((InternalFactHandle)fact).getId());
            }
            return new MatchDTO(match.getRule().getPackageName(), match.getRule().getName(), list);
        }

        public static MatchDTO create(String packageName, String ruleName, FactHandle... factHs){

            List<Integer> list = new ArrayList<>();
            for(FactHandle fact : factHs){
                list.add(((InternalFactHandle)fact).getId());
            }
            return new MatchDTO(packageName, ruleName, list);
        }



        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof MatchDTO)) {
                return false;
            }

            MatchDTO matchDTO = (MatchDTO) o;

            if (!getPkgName().equals(matchDTO.getPkgName())) {
                return false;
            }
            if (!getRuleName().equals(matchDTO.getRuleName())) {
                return false;
            }
            return getTuple().equals(matchDTO.getTuple());
        }

        @Override
        public int hashCode() {
            int result = getPkgName().hashCode();
            result = 31 * result + getRuleName().hashCode();
            result = 31 * result + getTuple().hashCode();
            return result;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("MatchDTO{");
            sb.append("pkgName='").append(pkgName).append('\'');
            sb.append(", ruleName='").append(ruleName).append('\'');
            sb.append(", tuple=").append(tuple);
            sb.append('}');
            return sb.toString();
        }
    }

}

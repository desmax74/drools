package org.drools.modelcompiler.u212;

import org.kie.api.runtime.rule.FactHandle;

public interface Persister {

    FactHandle add(Object o);

    void update(FactHandle factHandle,
                Object o);

    void remove(FactHandle factHandle);

    void fireAllRules();

    void fireAllRules(int index);

}

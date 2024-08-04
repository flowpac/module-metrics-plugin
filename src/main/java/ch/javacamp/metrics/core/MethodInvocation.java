package ch.javacamp.metrics.core;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

@Builder
@Getter
@Accessors(fluent = true)
public class MethodInvocation {

    private boolean isLocal;
    private String clazz;
    private String shortName;

    public String fqn(){
        return String.format("%s#%s", clazz, shortName);
    }

}

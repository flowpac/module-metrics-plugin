package ch.javacamp.metrics.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

class LCOM4Calculator {

    public int calculate(Set<MethodDescriptor> methodDescriptors){
        Set<Group> groups = new HashSet<>();

        for(var method:methodDescriptors){
            var methodNames = Relation.combine(Set.of(method.getShortName()), method.getInvokedLocalMethods());
            var fieldNames = Relation.combine(method.getReadFields(), method.getWrittenFields());
            groups.add(Group.create(fieldNames, methodNames));
        }

        boolean tryHarder = true;
        while(tryHarder){
            tryHarder = false;
            var localGroups = new ArrayList<>(groups);
            outerLoop:
            for(var g1:localGroups){
                for(var g2:localGroups) {
                    if (g1 != g2 && g1.intersect(g2)) {
                        tryHarder = true;
                        groups.remove(g2);
                        break outerLoop;
                    }
                }
            }
        }
        return groups.size();
    }



    static class Group {
        Set<String> fields;
        Set<String> methods;

        private Group(Set<String> fields, Set<String> methods){
            Objects.requireNonNull(fields);
            Objects.requireNonNull(methods);
            this.fields = fields;
            this.methods = methods;
        }

        public static Group create(Set<String> fields, Set<String> methods){
            return new Group(fields, methods);
        }

        public boolean hasCommonField(Set<String> otherFields){
            return Relation.containsAtLeastOneElement(fields, otherFields);
        }

        public boolean isConnected(Set<String> otherMethods){
            return Relation.containsAtLeastOneElement(methods, otherMethods);
        }

        public boolean intersect(Group other){
            if(this.hasCommonField(other.fields) || this.isConnected(other.methods)){
                this.fields.addAll(other.fields);
                this.methods.addAll(other.methods);
                return true;
            }
            return false;
        }
    }
}

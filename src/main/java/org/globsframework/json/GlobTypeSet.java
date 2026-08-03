package org.globsframework.json;

import org.globsframework.core.metamodel.GlobType;
import org.globsframework.core.metamodel.fields.*;
import org.globsframework.core.model.Glob;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class GlobTypeSet {
    public final GlobType[] globType;

    public GlobTypeSet(GlobType[] globType) {
        this.globType = globType;
    }

    public static GlobTypeSet export(GlobType globType) {
        Set<GlobType> types = new LinkedHashSet<>();
        add(globType, types);
        GlobType[] globTypes = new GlobType[types.size()];
        types.remove(globType);
        globTypes[0] = globType;
        int i = 1;
        for (GlobType type : types) {
            globTypes[i++] = type;
        }
        return new GlobTypeSet(globTypes);
    }

    private static void add(GlobType globType, Set<GlobType> types) {
        if (!types.add(globType)) {
            return;
        }
        globType.streamAnnotations().map(Glob::getType).forEach(t -> add(t, types));
        Field[] fields = globType.getFields();
        for (Field field : fields) {
            field.streamAnnotations().map(Glob::getType).forEach(t -> add(t, types));
            if (field instanceof GlobArrayField<?> arrayField) {
                add(arrayField.getTargetType(), types);
            }
            if (field instanceof GlobField<?> globField) {
                add((globField).getTargetType(), types);
            }
            if (field instanceof GlobUnionField) {
                Collection<GlobType> subType = ((GlobUnionField) field).getTargetTypes();
                for (GlobType type : subType) {
                    add(type, types);
                }
            }
            if (field instanceof GlobArrayUnionField) {
                Collection<GlobType> subType = ((GlobArrayUnionField) field).getTargetTypes();
                for (GlobType type : subType) {
                    add(type, types);
                }
            }
        }
    }
}

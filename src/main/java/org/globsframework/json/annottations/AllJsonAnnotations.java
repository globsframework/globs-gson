package org.globsframework.json.annottations;

import org.globsframework.core.metamodel.GlobModel;
import org.globsframework.core.metamodel.GlobTypeResolver;
import org.globsframework.core.metamodel.annotations.AllCoreAnnotations;
import org.globsframework.core.metamodel.impl.DefaultGlobModel;

public class AllJsonAnnotations {
    public final static GlobModel MODEL =
            new DefaultGlobModel(IsJsonContent.TYPE, JsonDateTimeFormat.TYPE, JsonDateFormat.TYPE, UnknownAnnotation.TYPE,
                    JsonHideValue.TYPE, JsonValueAsField.TYPE, JsonAsObject.TYPE,
                    JsonFlattenAttribute.TYPE, JsonFlattenTargetArray.TYPE, JsonFlatten.TYPE);

    public final static GlobTypeResolver RESOLVER = GlobTypeResolver.chain(AllCoreAnnotations.MODEL::findType,
            MODEL::findType);
}

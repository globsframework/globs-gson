package org.globsframework.json;

import org.globsframework.core.model.Glob;
import org.globsframework.core.model.MutableGlob;

public interface ToStringFieldJsonSerializer {
    String serialize(Glob value);
    void deserialize(String value, MutableGlob glob);
}

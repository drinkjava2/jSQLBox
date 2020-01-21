package com.github.drinkjava2.cglib.core.internal;

import java.util.*;

import com.github.drinkjava2.cglib.core.Customizer;
import com.github.drinkjava2.cglib.core.FieldTypeCustomizer;
import com.github.drinkjava2.cglib.core.KeyFactoryCustomizer;
@SuppressWarnings("all") // Yong
public class CustomizerRegistry {
    private final Class[] customizerTypes;
    private Map<Class, List<KeyFactoryCustomizer>> customizers = new HashMap<Class, List<KeyFactoryCustomizer>>();

    public CustomizerRegistry(Class[] customizerTypes) {
        this.customizerTypes = customizerTypes;
    }

    public void add(KeyFactoryCustomizer customizer) {
        Class<? extends KeyFactoryCustomizer> klass = customizer.getClass();
        for (Class type : customizerTypes) {
            if (type.isAssignableFrom(klass)) {
                List<KeyFactoryCustomizer> list = customizers.get(type);
                if (list == null) {
                    customizers.put(type, list = new ArrayList<KeyFactoryCustomizer>());
                }
                list.add(customizer);
            }
        }
    }

    public <T> List<T> get(Class<T> klass) {
        List<KeyFactoryCustomizer> list = customizers.get(klass);
        if (list == null) {
            return Collections.emptyList();
        }
        return (List<T>) list;
    }
    
    /**
     * @deprecated Only to keep backward compatibility.
     */
    @Deprecated
    public static CustomizerRegistry singleton(Customizer customizer)
    {
        CustomizerRegistry registry = new CustomizerRegistry(new Class[]{Customizer.class});
        registry.add(customizer);
        return registry;
    }
}

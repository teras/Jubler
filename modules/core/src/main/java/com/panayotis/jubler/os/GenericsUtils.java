/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.panayotis.jubler.os;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * /*
 * Based on ideas found here:
 * http://stackoverflow.com/questions/3403909/get-generic-type-of-class-at-runtime
 */
public class GenericsUtils {

    public static Class getTypeArgument(Field field, int index) {
        try {
            return getClass(((ParameterizedType) field.getGenericType()).getActualTypeArguments()[index]);
        } catch (Throwable t1) {
            try {
                return (Class) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[index];
            } catch (Throwable t2) {
                DEBUG.debug(t2);
                return null;
            }
        }
    }

    public static Class getTypeArgument(Type type, int index) {
        try {
            if (type instanceof Class)
                return (Class) type;
            if (type instanceof ParameterizedType) {
                return getClass(((ParameterizedType) type).getActualTypeArguments()[index]);
            }
        } catch (Throwable t1) {
        }
        return null;
    }

    public static Class<?> getClass(Type type) {
        if (type instanceof Class)
            return (Class) type;
        else if (type instanceof ParameterizedType)
            return getClass(((ParameterizedType) type).getRawType());
        else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> componentClass = getClass(componentType);
            if (componentClass != null)
                return Array.newInstance(componentClass, 0).getClass();
        } else if (type instanceof WildcardType) {
            Type[] bounds = ((WildcardType) type).getUpperBounds();
            if (bounds != null && bounds.length > 0)
                return getClass(bounds[0]);
            bounds = ((WildcardType) type).getLowerBounds();
            if (bounds != null && bounds.length > 0)
                return getClass(bounds[0]);
        }
        return null;
    }

    public static <T> Class<?> getTypeArgument(Class<T> baseClass, Class<? extends T> childClass, int index) {
        List<Class<?>> typeArguments = getTypeArguments(baseClass, childClass);
        return typeArguments == null || typeArguments.isEmpty() || Object.class.equals(typeArguments.get(index)) ? null : typeArguments.get(index);
    }
/*
 ((ParameterizedTypeImpl) ((Class) ((Class) ((Class) ((Class) ((Class) p.getPluginItems().iterator().next().getClass().getGenericSuperclass()).getGenericSuperclass()).getGenericSuperclass()).getGenericSuperclass()).getGenericSuperclass()).getGenericInterfaces()[0]).getActualTypeArguments()[0]
 */

    /**
     * Get the actual type arguments a child class has used to extend a generic
     * base class.
     *
     * @param <T>        The class type of the base class
     * @param baseClass  the base class
     * @param childClass the child class
     * @return a list of the raw classes for the actual type arguments.
     */
    public static <T> List<Class<?>> getTypeArguments(Class<T> baseClass, Class<? extends T> childClass) {
        Map<Type, Type> resolvedTypes = new HashMap<>();
        Type type = childClass;
        // start walking up the inheritance hierarchy until we hit baseClass
        while (!getClass(type).equals(baseClass))
            if (type instanceof Class)
                // there is no useful information for us in raw types, so just keep going.
                type = ((Class) type).getGenericSuperclass();
            else {
                ParameterizedType parameterizedType = (ParameterizedType) type;
                Class<?> rawType = (Class) parameterizedType.getRawType();

                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
                for (int i = 0; i < actualTypeArguments.length; i++)
                    resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);

                if (!rawType.equals(baseClass))
                    type = rawType.getGenericSuperclass();
            }

        // finally, for each actual type argument provided to baseClass, determine (if possible)
        // the raw class for that type argument.
        Type[] actualTypeArguments;
        if (type instanceof Class)
            actualTypeArguments = ((Class) type).getTypeParameters();
        else
            actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
        List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
        // resolve types by chasing down type variables.
        for (Type baseType : actualTypeArguments) {
            while (resolvedTypes.containsKey(baseType))
                baseType = resolvedTypes.get(baseType);
            typeArgumentsAsClasses.add(getClass(baseType));
        }
        return typeArgumentsAsClasses;
    }


    public static List<Class<?>> getInterfaceTypeArguments(Class<?> interfaceClass, Class<?> childClass) {
        while (childClass != null) {
            for (Type interf : childClass.getGenericInterfaces()) {
                if (interf instanceof ParameterizedType) {
                    ParameterizedType interfType = (ParameterizedType) interf;
                    if (interfType.getRawType().equals(interfaceClass)) {
                        return Arrays.stream(interfType.getActualTypeArguments())
                                .map(GenericsUtils::getClass)
                                .collect(Collectors.toList());
                    }
                }
            }
            childClass = childClass.getSuperclass();
        }
        return Collections.emptyList();
    }
}

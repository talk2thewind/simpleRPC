package provider.register;

import java.util.HashMap;
import java.util.Map;

public class LocalRegister {
    private static final Map<Class<?>, Class<?>> interfaceToClassMap = new HashMap<>();

    public static void register(Class<?> interfaceClass, Class<?> implClass) {
        interfaceToClassMap.put(interfaceClass, implClass);
        System.out.println("本地注册服务实现类：" + interfaceClass.getName() + " -> " + implClass.getName());
    }

    public static Class<?> get(Class<?> interfaceClass) {
        return interfaceToClassMap.get(interfaceClass);
    }
}

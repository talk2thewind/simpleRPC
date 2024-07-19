package api.common;

import java.io.Serial;
import java.io.Serializable;

public class Invocation implements Serializable {//处理请求序列化

    @Serial
    private static final long serialVersionUID = 1L;
    private String interfaceName;
    private String methodName;
    private Class[] parameterTypes;
    private Object[] parameters;

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Class[] getParameterTypes() {
        return parameterTypes;
    }

    public void setParameterTypes(Class[] parameterTypes) {
        this.parameterTypes = parameterTypes;
    }

    public Object[] getParameters() {
        return parameters;
    }

    public void setParameters(Object[] parameters) {
        this.parameters = parameters;
    }

    public Invocation(String interfaceName, String methodName, Class[] parameterTypes, Object[] parameters) {
        this.interfaceName = interfaceName;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.parameters = parameters;
    }
}

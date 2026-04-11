package ru.yandex.mylogininya;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Aspect
public class AnalyzingAspect {

    static final Object lock = new Object();

    void scanObject(Object o, ManyConsumer<Object, Integer, String, Boolean, String, String> logger, StringBuilder s) {
        int len = 0;

        List<Object> subfields = new ArrayList<>();
        List<Integer> subfieldDepth = new ArrayList<>();
        List<String> fieldNames = new ArrayList<>();
        List<String> baseTypes = new ArrayList<>();
        List<String> fieldTypes = new ArrayList<>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        subfields.add(o);
        subfieldDepth.add(0);
        fieldNames.add("object");
        baseTypes.add("-");
        fieldTypes.add("-");
        while (!subfields.isEmpty()) {
            Object current = subfields.remove(subfields.size() - 1);
            int depth = subfieldDepth.remove(subfieldDepth.size() - 1);
            String fieldName = fieldNames.remove(fieldNames.size() - 1);
            String baseType = baseTypes.remove(baseTypes.size() - 1);
            String thisType = fieldTypes.remove(fieldTypes.size() - 1);
            logger.accept(current, depth, fieldName, visited.containsKey(current), baseType, thisType);

            if (len == 0) {
                len = s.length();
            }

            if (visited.containsKey(current) || current == null) {
                continue;
            }
            visited.put(current, null);
            Class<?> cls = current.getClass();
            if (cls.isArray()) {
                Class<?> cmpType = cls.getComponentType();
                if (!cmpType.isPrimitive()) {
                    int length = Array.getLength(current);
                    for (int i = 0; i < length; i++) {
                        Object elem = Array.get(current, i);
                        subfields.add(elem);
                        subfieldDepth.add(depth + 1);
                        fieldNames.add("[*]");
                        baseTypes.add("-");
                        fieldTypes.add("-");
                    }
                }
                continue;
            }
            Field[] fields = cls.getDeclaredFields();
            for (Field field : fields) {
                Class<?> type = field.getType();
                if (!type.isPrimitive() && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        field.setAccessible(true);
                        Object subObj = field.get(current);
                        subfields.add(subObj);
                        subfieldDepth.add(depth + 1);
                        fieldNames.add(field.getName());
                        baseTypes.add(cls.getName());
                        fieldTypes.add(type.getName());
                    } catch (Exception e) {
                        throw new RuntimeException("incorrect use of reflection");
                    }
                }
            }
        }

        if (s.length() - len > 50 * 800) { // ~ 800 lines
            s.setLength(len);
        }
    }

    void printObject(Object o, Consumer<String> printer, StringBuilder s) {
        ManyConsumer<Object, Integer, String, Boolean, String, String> printerValue = (obj, depth, field, isRepeated, baseType, thisType) -> {
            String objectString = Integer.toHexString(System.identityHashCode(obj));
            printer.accept(field + " " + objectString + " " + depth + " " + ((isRepeated) ? "repeated" : "new") + " " + baseType + " " + thisType + "\n");
        };
        scanObject(o, printerValue, s);
    }

    static final HashMap<String, Integer> counterFuncs = new HashMap<>();

    @Pointcut("execution(public * java.util.ArrayList$ArrayListSpliterator#trySplit()(..))" + " || " +
"execution(public * java.util.ArrayList$ArrayListSpliterator#tryAdvance(java.util.function.Consumer)(..))" + " || " +
"execution(public * java.util.ArrayList$ArrayListSpliterator#forEachRemaining(java.util.function.Consumer)(..))" + " || " +
"execution(public * java.util.ArrayList$ArrayListSpliterator#estimateSize()(..))" + " || " +
"execution(public * java.util.ArrayList$ArrayListSpliterator#characteristics()(..))" + " || " +
"execution(public * java.util.ArrayList$Itr#hasNext()(..))" + " || " +
"execution(public * java.util.ArrayList$Itr#next()(..))" + " || " +
"execution(public * java.util.ArrayList$Itr#remove()(..))" + " || " +
"execution(public * java.util.ArrayList$Itr#forEachRemaining(java.util.function.Consumer)(..))" + " || " +
"execution(public * java.util.ArrayList$ListItr#hasPrevious()(..))" + " || " +
"execution(public * java.util.ArrayList$ListItr#nextIndex()(..))" + " || " +
"execution(public * java.util.ArrayList$ListItr#previousIndex()(..))" + " || " +
"execution(public * java.util.ArrayList$ListItr#previous()(..))" + " || " +
"execution(public * java.util.ArrayList$ListItr#set(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$ListItr#add(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#hasNext()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#next()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#hasPrevious()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#previous()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#forEachRemaining(java.util.function.Consumer)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#nextIndex()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#previousIndex()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#remove()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#set(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$1#add(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$2#trySplit()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$2#tryAdvance(java.util.function.Consumer)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$2#forEachRemaining(java.util.function.Consumer)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$2#estimateSize()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList$2#characteristics()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#<init>(java.util.ArrayList,int,int)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#set(int,java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#get(int)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#size()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#add(int,java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#remove(int)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#addAll(java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#addAll(int,java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#replaceAll(java.util.function.UnaryOperator)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#removeAll(java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#retainAll(java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#removeIf(java.util.function.Predicate)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#toArray()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#toArray(java.lang.Object[])(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#equals(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#hashCode()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#indexOf(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#lastIndexOf(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#contains(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#iterator()(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#listIterator(int)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#subList(int,int)(..))" + " || " +
"execution(public * java.util.ArrayList$SubList#spliterator()(..))" + " || " +
"execution(public * java.util.ArrayList#<init>(int)(..))" + " || " +
"execution(public * java.util.ArrayList#<init>()(..))" + " || " +
"execution(public * java.util.ArrayList#<init>(java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList#trimToSize()(..))" + " || " +
"execution(public * java.util.ArrayList#ensureCapacity(int)(..))" + " || " +
"execution(public * java.util.ArrayList#size()(..))" + " || " +
"execution(public * java.util.ArrayList#isEmpty()(..))" + " || " +
"execution(public * java.util.ArrayList#contains(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#indexOf(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#lastIndexOf(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#clone()(..))" + " || " +
"execution(public * java.util.ArrayList#toArray()(..))" + " || " +
"execution(public * java.util.ArrayList#toArray(java.lang.Object[])(..))" + " || " +
"execution(public * java.util.ArrayList#get(int)(..))" + " || " +
"execution(public * java.util.ArrayList#set(int,java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#add(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#add(int,java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#remove(int)(..))" + " || " +
"execution(public * java.util.ArrayList#equals(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#hashCode()(..))" + " || " +
"execution(public * java.util.ArrayList#remove(java.lang.Object)(..))" + " || " +
"execution(public * java.util.ArrayList#clear()(..))" + " || " +
"execution(public * java.util.ArrayList#addAll(java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList#addAll(int,java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList#removeAll(java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList#retainAll(java.util.Collection)(..))" + " || " +
"execution(public * java.util.ArrayList#listIterator(int)(..))" + " || " +
"execution(public * java.util.ArrayList#listIterator()(..))" + " || " +
"execution(public * java.util.ArrayList#iterator()(..))" + " || " +
"execution(public * java.util.ArrayList#subList(int,int)(..))" + " || " +
"execution(public * java.util.ArrayList#forEach(java.util.function.Consumer)(..))" + " || " +
"execution(public * java.util.ArrayList#spliterator()(..))" + " || " +
"execution(public * java.util.ArrayList#removeIf(java.util.function.Predicate)(..))" + " || " +
"execution(public * java.util.ArrayList#replaceAll(java.util.function.UnaryOperator)(..))" + " || " +
"execution(public * java.util.ArrayList#sort(java.util.Comparator)(..))")
    public void specificMethods() {}

    final static int MAX_FUN_RECORDS = 20;

    @Around("specificMethods()")
    public Object analyzeAround(ProceedingJoinPoint joinPoint) throws Throwable {
        StringBuilder dump = new StringBuilder();

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
//        System.out.print("*");
        String methodName = signature.getName()
                + "(" + Arrays.stream(signature.getParameterTypes())
                .map(Class::getCanonicalName)
                .collect(Collectors.joining(",")) + ")";
        String className = joinPoint.getSignature().getDeclaringTypeName();
        Object[] args = joinPoint.getArgs();
        String classMethodStr = className + "::" + methodName + "\n";
        boolean needLog = true;
        synchronized (lock) {
            if (counterFuncs.getOrDefault(classMethodStr, 0) >= MAX_FUN_RECORDS) {
                needLog = false;
            } else {
                counterFuncs.compute(classMethodStr, (k, v) -> (v == null) ? 1 : v + 1);
            }
        }

        if (needLog) {
            dump.append("-------------------------------" + " " + Thread.currentThread().getId() + " " + ProcessHandle.current().pid() + "\n");
            dump.append("BEFORE:\n");
            dump.append(classMethodStr);
            Object target = joinPoint.getTarget();
            dump.append("this\n");
            printObject(target, dump::append, dump);
            int i = 0;
            for (Object o : args) {
                dump.append(i + "\n");
                i++;
                printObject(o, dump::append, dump);
            }
            dump.append("-------------------------------" + " " + Thread.currentThread().getId() + " " + ProcessHandle.current().pid() + "\n"); // TODO
        }

        Object result = joinPoint.proceed();

        if (needLog) {
            Object[] argsAfter = joinPoint.getArgs();
            dump.append("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@" + " " + Thread.currentThread().getId() + " " + ProcessHandle.current().pid() + "\n");
            dump.append("AFTER:\n");
            dump.append(classMethodStr);
            Object target = joinPoint.getTarget();
            dump.append("this\n");
            printObject(target, str -> dump.append(str), dump);
            int i = 0;
            for (Object o : argsAfter) {
                dump.append(i + "\n");
                i++;
                printObject(o, dump::append, dump);
            }
            dump.append("RV\n");
            printObject(result, dump::append, dump);
            dump.append("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@" + Thread.currentThread().getId() + " " + ProcessHandle.current().pid() + "\n");

            synchronized (lock) {
                try (PrintWriter w = new PrintWriter(new FileWriter("/mnt/data/MyOwnFolder/learning/p_algo/aspectAdd/aspectAdd/aspectDump/dump" + ProcessHandle.current().pid() + ".txt", true))) {
                    w.print(dump.toString());
                    w.flush();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return result;
    }
    // grep -o "accessible: module java.base does not.*" output.txt | cut -d' ' -f6-7 | sort | uniq
    // cat output.txt | grep -B 30 -A 10 "Caused by" | less
}
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

    @Pointcut("execution(public * org.apache.logging.log4j.core.test.layout.Log4j2_1482_Test.loopingRun(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.MapMessage.put(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.Timer.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ObjectMessage.formatTo(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ParameterizedMessage.formatTo(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ParameterizedMessageTest.verifyFormattingFailureOnInsufficientArgs(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ReusableMessageFactory.release(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ReusableMessageFactoryTest.assertReusableParameterizeMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ReusableObjectMessage.set(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ReusableObjectMessage.formatTo(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ReusableParameterizedMessage.forEachParameter(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ReusableSimpleMessage.set(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ReusableSimpleMessage.formatTo(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.SimpleMessage.formatTo(..))" + " || " +
"execution(public * org.apache.logging.log4j.message.ThreadDumpMessage.formatTo(..))" + " || " +
"execution(public * org.apache.logging.log4j.AbstractLoggerTest$CountingLogger.setCurrentLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.AbstractLoggerTest$CountingLogger.setCurrentEvent(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.debug(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.error(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.fatal(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.info(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.log(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.trace(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.warn(..))" + " || " +
"execution(public * org.apache.logging.log4j.CloseableThreadContext$Instance.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.put(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.push(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.putAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.EventLogger.logEvent(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.clearMap(..))" + " || " +
"execution(public * org.apache.logging.log4j.internal.map.UnmodifiableArrayBackedMap.forEach(..))" + " || " +
"execution(public * org.apache.logging.log4j.internal.map.UnmodifiableArrayBackedMap.putAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.internal.map.UnmodifiableArrayBackedMap.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.error(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.spi.ThreadContextMapSuite.singleValue(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.DefaultThreadContextMap.putAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.DefaultThreadContextMap.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.spi.ThreadContextMapSuite.getCopyReturnsMutableCopy(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.spi.ThreadContextMapSuite.getImmutableMapReturnsNullIfEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.spi.ThreadContextMapSuite.getImmutableMapReturnsImmutableMapIfNonEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.spi.ThreadContextMapSuite.getImmutableMapCopyNotAffectedByContextMapChanges(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.DefaultThreadContextMap.put(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.DefaultThreadContextMap.remove(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.spi.ThreadContextMapSuite.threadLocalNotInheritableByDefault(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.spi.ThreadContextMapSuite.threadLocalInheritableIfConfigured(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.DefaultThreadContextStack.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.DefaultThreadContextStack.push(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.DefaultThreadContextStack.trim(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.LoggerAdapterTest$TestLoggerContext2.shutdown(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.LoggerRegistry.putIfAbsent(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.MutableThreadContextStack.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.MutableThreadContextStack.push(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.MutableThreadContextStack.trim(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.MutableThreadContextStack.freeze(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusConsoleListener.log(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusConsoleListener.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusConsoleListener.setStream(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusConsoleListener.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusLoggerDateTest.verifyFormatter(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusLoggerDateTest.verifyInvalidDateFormatAndZone(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusListener.log(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusLogger.registerListener(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusLogger.removeListener(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusLogger.reset(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.TestProperties.setProperty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.perfTest(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.testGetContextReturnsEmptyMapIfEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.testGetContextReturnsMutableCopy(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.testGetImmutableContextReturnsEmptyMapIfEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfNonEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.testGetImmutableContextReturnsImmutableMapIfEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.testGetImmutableStackReturnsEmptyStackIfEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ThreadContextUtilityClass.testPut(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.remove(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.putIfNull(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.removeAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.traceExit(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.PropertiesUtilTest.assertHasAllProperties(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.PropertiesUtil.addPropertySource(..))" + " || " +
"execution(public * org.apache.logging.log4j.test.ListStatusListener.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.PropertiesUtil.removePropertySource(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.ProviderUtilTest.assertHasErrorOrWarning(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.ProviderUtilTest.assertNoErrorsOrWarnings(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.SortedArrayStringMap.putValue(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.SortedArrayStringMap.putAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.SortedArrayStringMap.freeze(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.SortedArrayStringMap.remove(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.SortedArrayStringMap.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.SortedArrayStringMap.forEach(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.StringBuilders.trimToMaxSize(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.StringBuilders.escapeJson(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.StringBuilders.escapeXml(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.SystemPropertiesPropertySource.forEach(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.Unbox1Test.populate(..))" + " || " +
"execution(public * org.apache.logging.log4j.LogBuilder.log(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.entry(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.exit(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.catching(..))" + " || " +
"execution(public * org.apache.logging.log4j.LoggerTest.assertMessageFactoryInstanceOf(..))" + " || " +
"execution(public * org.apache.logging.log4j.LoggerTest.assertEqualMessageFactory(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.AbstractLogger.printf(..))" + " || " +
"execution(public * org.apache.logging.log4j.LogManagerTest$1.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.LogManagerTest$2.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.jpl.Log4jSystemLoggerTest.testMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.StringMap.putValue(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.Logger.addAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.filter.AbstractFilterable.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerContext.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LifeCycle.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LifeCycle.stop(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.util.PluginManager.addPackage(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configuration.addListener(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.info(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.AbstractLifeCycle.stop(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.Logger.removeAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.java9.StackLocatorTest$Inner.access$000(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.java9.StackLocatorTest$Inner.access$100(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.clearAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.debug(..))" + " || " +
"execution(public * org.apache.logging.log4j.FilterPerformanceComparison.testPerformance(..))" + " || " +
"execution(public * org.apache.logging.log4j.FilterPerformanceComparison.testThreads(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.test.util.Profiler.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.test.util.Profiler.stop(..))" + " || " +
"execution(public * org.apache.logging.log4j.PerformanceComparison.doRun(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.AbstractAppender.setHandler(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ConsoleAppenderTest.testFollowSystemPrintStream(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ConsoleAppenderTest.testConsoleStreamManagerDoesNotClose(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.CsvJsonParameterLayoutFileAppenderTest.testNoNulCharacters(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.AbstractConfiguration.addAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.AbstractConfiguration.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.warn(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.fatal(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.AsyncAppenderTest.rewriteTest(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.AsyncAppenderTest.exceptionTest(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiStyleLayoutMain.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.AbstractOutputStreamAppender.append(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.FileAppenderTest.writer(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.FileAppenderTest.verifyFile(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.FileAppenderTest.testMultipleLockingAppenderThreads(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.HttpAppender.append(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.InMemoryAppenderTest.assertMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.MemoryMappedFileManager.write(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.AbstractManager.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.OutputStreamManager.writeBytes(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.mom.jeromq.JeroMqAppenderTest.addLoggingFilter(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.mom.jeromq.JeroMqAppender.resetSendRcs(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.mom.JmsAppender.append(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.Appender.append(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerContext.reconfigure(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.nosql.NoSqlDatabaseManager.connectAndStart(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.nosql.NoSqlDatabaseManager.writeInternal(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.startup(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.nosql.NoSqlConnection.insertObject(..))" + " || " +
"execution(public * org.apache.logging.log4j.ThreadContext.clearStack(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.OutputStreamAppenderTest.addAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configuration.addAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ConfigurationTestUtils.updateLoggers(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.OutputStreamManager.write(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.RandomAccessFileManager.flush(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ReconfigureAppenderTest.createAndAddAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ReconfigureAppenderTest.removeAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ReconfigureAppenderTest.removeManagerUsingReflection(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rewrite.MapRewritePolicyTest.compareLogEvents(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rewrite.MapRewritePolicyTest.checkAdded(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rewrite.MapRewritePolicyTest.checkUpdated(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ConsoleAppenderAnsiXExceptionMain.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppenderTest.setUp(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender.append(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.writeThrough(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender.replaceManager(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.startupInternal(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManagerTest.setUp(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.write(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.connectAndStart(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.writeInternal(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.buffer(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.AbstractDatabaseManager.flush(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.trace(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LifeCycle.initialize(..))" + " || " +
"execution(public * org.apache.logging.log4j.status.StatusLogger.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.AbstractAction.run(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.IfAccumulatedFileCount.beforeFileTreeWalk(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.IfAccumulatedFileSize.beforeFileTreeWalk(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.IfAll.beforeFileTreeWalk(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.IfAny.beforeFileTreeWalk(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.IfFileName.beforeFileTreeWalk(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.IfLastModified.beforeFileTreeWalk(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.action.IfNot.beforeFileTreeWalk(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.AbstractConfiguration.initialize(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.CronTriggeringPolicyTest.testBuilder(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.CronTriggeringPolicyTest.testFactoryMethod(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingFileManager.initialize(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.PatternProcessor.formatFileName(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderCronOnStartupTest.cleanDir(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderDeleteAccumulatedCount1Test.updateLastModified(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderDeleteAccumulatedCount2Test.updateLastModified(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderDeleteNestedTest.updateLastModified(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderDirectCronTest$RolloverDelay.waitForRollover(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderDirectCronTest$RolloverDelay.reset(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingFileManager.addRolloverListener(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderDirectWriteWithHtmlLayoutTest.checkAppenderWithHtmlLayout(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerContext.setConfigLocation(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingAppenderRestartTest.validate(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.RollingFileAppender.append(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.test.junit.LoggerContextRule.reconfigure(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingFileAppenderUpdateDataTest.validateAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingFileManager.writeToDestination(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingFileManager.rollover(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingFileManager.createParentDir(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingFileManager.setRenameEmptyFiles(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager.write(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager.flush(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.FileUtils.defineFilePosixAttributeView(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.routing.DefaultRouteScriptAppenderTest.logAndCheck(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.routing.DefaultRouteScriptAppenderTest.checkStaticVars(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.routing.RoutesScriptAppenderTest.logAndCheck(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.routing.RoutesScriptAppenderTest.checkStaticVars(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.routing.RoutingAppenderWithPurgingTest.assertFileExistance(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.routing.RoutingAppender.deleteAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.ScriptAppenderSelectorTest.verify(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SmtpAppenderAsyncTest.testSmtpAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.Logger.setAdditive(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.Logger.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.test.smtp.SimpleSmtpServer.stop(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SocketAppenderTest.testTcpAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.LineReadingTcpServer.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.Appender.setHandler(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SocketAppenderReconnectTest.verifyLoggingSuccess(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.LineReadingTcpServer.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SocketAppenderReconnectTest.verifyLoggingFailure(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.net.TcpSocketManager.setHostResolver(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SocketAppenderTest$TcpSocketTestServer.shutdown(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SyslogAppenderTest.initTCPTestEnvironment(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SyslogAppenderTestBase.sendAndCheckLegacyBsdMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SyslogAppenderTestBase.sendAndCheckStructuredMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SyslogAppenderTest.initUDPTestEnvironment(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.TlsSyslogAppenderTest.initTlsTestEnvironment(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SyslogAppenderTestBase.sendAndCheckLegacyBsdMessages(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.SyslogAppenderTestBase.sendAndCheckStructuredMessages(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.WriterAppenderTest.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.test.CoreLoggerContexts.stopLoggerContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.traceExit(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.LoggerConfig.addAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.async.AsyncLoggerConfigDisruptor.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.LoggerConfig.log(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.ExtendedLogger.logMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configuration.setNanoClock(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.async.AsyncLogger.updateConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.async.AbstractAsyncThreadContextTestBase.testAsyncLogWritesToLog(..))" + " || " +
"execution(public * org.apache.logging.log4j.LogManager.shutdown(..))" + " || " +
"execution(public * org.apache.logging.log4j.spi.ExtendedLogger.logIfEnabled(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.async.QueueFullAbstractTest.testNormalQueueFullKeepsMessagesInOrder(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.GcHelper.awaitGarbageCollection(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.async.RingBufferLogEvent.setValues(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.async.RingBufferLogEvent.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.util.StringMap.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.test.Compiler.compile(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.GraalVmProcessor.init(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.PluginCacheTest.createCategory(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.PluginProcessorTest.verifyFakePluginEntry(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.util.PluginManager.addPackages(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.util.PluginManager.clearPackages(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.util.PluginManager.collectPlugins(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.PluginEntry.setKey(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.PluginEntry.setClassName(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.PluginEntry.setName(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.PluginEntry.setCategory(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.processor.PluginCache.writeCache(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.util.ResolverUtil.setClassLoader(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.util.ResolverUtil.findInPackage(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.plugins.util.ResolverUtilTest.testExtractPathFromJarUrlNotDecodedIfFileExists(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.AdvertiserTest.verifyExpectedEntriesAdvertised(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerContext.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.builder.ConfigurationAssemblerTest.validate(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.builder.ConfigurationBuilderTest.addTestFixtures(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.CompositeConfigurationTest.runTest(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.ConfigurationFactoryTest.checkConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.ConfigurationFactoryTest.checkFileLogger(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configurator.shutdown(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configurator2Test.testInitializeFromFilePath(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configurator.reconfigure(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configurator.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configuration.addLogger(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerContext.updateLoggers(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.LoggerConfig.setLogEventFactory(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.LoggerConfig.setParent(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.MultipleTriggeringPolicyTest.assertBothTriggeringPoliciesConfigured(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.PropertyTest.verifyProperty(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.ReconfigurationDeadlockTest.updateConfigFileModTime(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.MockReliabilityStrategy.rethrowAssertionErrors(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.xml.XmlConfigurationPropsTest.testConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.context.internal.GarbageFreeSortedArrayThreadContextMap.putAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.context.internal.GarbageFreeSortedArrayThreadContextMap.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.test.GcFreeLoggingTestUtil.runTest(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap.putValue(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap.putAll(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap.forEach(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap.freeze(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap.remove(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.JdkMapAdapterStringMap.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.Log4jLogEvent.setNanoClock(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.Log4jLogEventTest.verifyNanoTimeWithAllConstructors(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setContextData(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setContextStack(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setEndOfBatch(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setIncludeLocation(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setLoggerFqcn(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setLoggerName(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setMarker(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setNanoTime(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setThreadName(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setThrown(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setTimeMillis(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.Log4jLogEventTest.different(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.initFrom(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setThreadId(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setThreadPriority(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.MutableLogEvent.setSource(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.ReusableLogEventFactory.release(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.ThreadContextDataInjectorTest.prepareThreadContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.ThreadContextDataInjectorTest.testContextDataInjector(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.impl.ThrowableFormatOptionsTest.testFullAnsiEmptyConfig(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.jackson.LevelMixInTest.testNameOnly(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.jackson.StackTraceElementMixInTest.roundtrip(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.CsvLogEventLayoutTest.testLayout(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.CsvParameterLayoutTest.testLayoutNormalApi(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.GelfLayoutTest.testCompressedLayout(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.GelfLayoutTest.testRequiresLocation(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.HtmlLayoutTest.testLayout(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.HtmlLayoutTest.testLayoutWithDatePatternFixedFormat(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.JsonLayoutTest.checkAt(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.JsonLayoutTest.checkContains(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.JsonLayoutTest.testAllFeatures(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.PatternLayoutTest.assertToByteArray(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.PatternLayoutTest.assertEncode(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.lookup.MainMapLookup.setMainArguments(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.PatternLayoutTest.testMdcPattern(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.Rfc5424LayoutTest.checkDefaultValues(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.SerializedLayoutTest.testSerialization(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.StringBuilderEncoder.encode(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.XmlLayoutTest.checkContains(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.XmlLayoutTest.testAllFeatures(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.YamlLayoutTest.checkAt(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.YamlLayoutTest.checkContains(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.layout.YamlLayoutTest.testAllFeatures(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerMessageFactoryCustomizationTest.assertTestMessageFactories(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerTest.assertEventCount(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.catching(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configurator.setAllLevels(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.Configurator.setRootLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.entry(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.LoggerConfig.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.LoggerContext.addPropertyChangeListener(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.message.ExtendedThreadInformationTest.obtainMessageWithMissingStackTrace(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.MonitorResourcesTest.assertMonitorResourceFileNames(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.net.SmtpManagerTest.testAdd(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.net.ssl.KeyStoreConfigurationTest.checkKeystoreConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.net.ssl.SslConfigurationFactoryTest.addKeystoreConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.net.ssl.SslConfigurationFactoryTest.addTruststoreConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.parser.LogEventParserTest.assertLogEvent(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.DatePatternConverterTestBase.assertDatePattern(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.DatePatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.time.MutableInstant.initFromEpochMilli(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.DynamicWordAbbreviator.abbreviate(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.EncodingPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.EndOfBatchPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.EqualsIgnoreCaseReplacementConverterTest.testReplacement(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest$AbstractStackTraceTest.assertStackTraceLines(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.FormattingInfo.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.HighlightConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.JAnsiTextRenderer.render(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.LevelPatternConverterTest.testLevelLength(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.LevelPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.LoggerFqcnPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.MapPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.MarkerPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.MaxLengthConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.MdcPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.MessagePatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.NameAbbreviator.abbreviate(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.time.MutableInstant.initFromEpochSecond(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.NanoTimePatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.NdcPatternConverterTest.testConverter(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.PatternParserTest.validateConverter(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.PatternFormatter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.PatternParserTest.testNestedPatternHighlight(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.PatternParserTest.testFirstConverter(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.PatternParserTest.testThreadNamePattern(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.PatternParserTest.testThreadIdPattern(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.PatternParserTest.testThreadPriorityPattern(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.RepeatPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.StyleConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.ThreadIdPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.ThreadNamePatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.ThreadPriorityPatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.ThrowablePatternConverterTest$AbstractPropertyTest.assertConversion(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.VariablesNotEmptyReplacementConverterTest.testReplacement(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.EqualsReplacementConverterTest.testReplacement(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.EqualsReplacementConverterTest.testParseSubstitution(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.MarkerSimpleNamePatternConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.pattern.RegexReplacementConverter.format(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.filter.AbstractFilterable.addFilter(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.filter.AbstractFilterable.removeFilter(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.filter.AbstractFilterTest.verifyMethodsWithUnrolledVarargs(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.filter.BurstFilter.clear(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.AbstractLifeCycle.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.filter.MutableThreadContextMapFilter.registerListener(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.ConfigurationAware.setConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.lookup.StrSubstitutor.setEnableSubstitutionInVariables(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.lookup.InterpolatorTest.assertLookupNotEmpty(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.lookup.Interpolator.setConfiguration(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.lookup.Interpolator.setLoggerContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.time.MutableInstant.initFrom(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.time.MutableInstant.instantToMillisAndNanos(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.CyclicBuffer.add(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.config.HttpWatcher.watching(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.AbstractWatcher.modified(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.internal.instant.InstantPatternDynamicFormatterTest.assertPatternPrecision(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.internal.instant.InstantFormatter.formatTo(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.internal.instant.InstantPatternThreadLocalCachedFormatterTest.formatOnNewThread(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.JsonUtils.quoteAsString(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.ShutdownCallbackRegistryTest$Registry.access$100(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.WatchManager.watchFile(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.WatchManager.reset(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.WatchManager.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.WatchManager.watch(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.util.Watcher.modified(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.XmlEvents$TransferMessage.setCompletionStatus(..))" + " || " +
"execution(public * org.apache.logging.log4j.io.AbstractStreamTest.assertMessages(..))" + " || " +
"execution(public * org.apache.logging.log4j.io.IoBuilderCallerInfoTesting.assertMessages(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.mom.jakarta.JmsAppender.append(..))" + " || " +
"execution(public * org.apache.logging.log4j.smtp.SmtpAppenderAsyncTest.testSmtpAppender(..))" + " || " +
"execution(public * org.apache.logging.log4j.smtp.SmtpManagerTest.testAdd(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jServletContainerInitializer.onStartup(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebLifeCycle.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebSupport.setLoggerContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jServletContextListener.contextInitialized(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jServletContextListener.contextDestroyed(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebSupport.clearLoggerContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebLifeCycle.stop(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jServletContextListenerTest.ensureInitializingFailsWhenAuthShutdownIsEnabled(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jServletFilter.init(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jServletFilter.destroy(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jServletFilter.doFilter(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jShutdownOnContextDestroyedListenerTest.setUp(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jShutdownOnContextDestroyedListener.contextInitialized(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jShutdownOnContextDestroyedListener.contextDestroyed(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebInitializerImpl.setLoggerContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebInitializerImpl.clearLoggerContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebInitializerImpl.start(..))" + " || " +
"execution(public * org.apache.logging.log4j.web.Log4jWebInitializerImpl.wrapExecution(..))" + " || " +
"execution(public * org.apache.logging.log4j.jcl.LoggerTest.verify(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.jdbc.PoolingDriverConnectionSourceTest.openAndClose(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.jpa.AbstractJpaAppenderTest.setUp(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.db.jpa.AbstractJpaAppenderTest.tearDown(..))" + " || " +
"execution(public * org.apache.logging.log4j.jul.test.AbstractLoggerTest.testMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.jul.test.AbstractLoggerTest.testLambdaMessages(..))" + " || " +
"execution(public * org.apache.logging.log4j.jul.test.Log4jBridgeHandlerTest.assertSysoutMatches(..))" + " || " +
"execution(public * org.apache.logging.log4j.jul.test.Log4jBridgeHandlerTest.subMethodWithLogs(..))" + " || " +
"execution(public * org.apache.logging.log4j.jul.test.Log4jBridgeHandlerTest.assertLogLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.jul.test.Log4jBridgeHandlerTest.test5LevelPropFromConfigFile(..))" + " || " +
"execution(public * org.apache.logging.log4j.jul.test.Log4jBridgeHandlerTest.debugPrintJulLoggers(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.EcsLayoutTest.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.TestHelpers.usingSerializedLogEventAccessor(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.GelfLayoutTest.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonLayoutTest.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutAdditionalFieldTest.assertAdditionalFields(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutConcurrentEncodeTest.withContextFromTemplate(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutConcurrentEncodeTest.verifyLines(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.log(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutTest.checkLogEvent(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutTest.test_lineSeparator_suffix(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayout.encode(..))" + " || " +
"execution(public * org.apache.logging.log4j.core.appender.OutputStreamManager.flush(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutTest$JsonAcceptingTcpServer.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutTest.testMessageParameterResolver(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.JsonTemplateLayoutTest.testMessageParameterResolverNoParameters(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.LogstashIT.testEvents(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.LogstashIT.deleteIndex(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.resolver.CounterResolverTest.verify(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.resolver.ReadOnlyStringMapResolverTest.verifyConfigFailure(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.resolver.ReadOnlyStringMapResolverTest.testReadOnlyStringMapKeyAccess(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.resolver.ReadOnlyStringMapResolverTest.testReadOnlyStringMapPattern(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.resolver.ReadOnlyStringMapResolverTest.testReadOnlyStringMapFlatten(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.resolver.StackTraceStringResolverTest$AbstractTestCases.assertSerializedException(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.TruncatingBufferedPrintWriter.close(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.TestHelpers.withContextFromTemplate(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.CharSequencePointerTest.assertMissingReset(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.CharSequencePointer.reset(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.CharSequencePointerTest.assertUnsupportedOperation(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.JsonReaderTest.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.JsonWriterTest.withLockedWriter(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.JsonWriterTest.expectNull(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.JsonWriterTest.testQuoting(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.StringParameterParserTest.testSuccess(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.TruncatingBufferedWriter.write(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.TruncatingBufferedWriterTest.verifyClose(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.TruncatingBufferedWriterTest.verifyTruncation(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.TruncatingBufferedWriter.position(..))" + " || " +
"execution(public * org.apache.logging.log4j.layout.template.json.util.TruncatingBufferedWriterTest.assertUnsupportedOperation(..))" + " || " +
"execution(public * org.apache.logging.log4j.mongodb.AbstractMongoDbCappedIT.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.mongodb4.AbstractMongoDb4CappedIT.test(..))" + " || " +
"execution(public * org.apache.logging.log4j.mongodb4.MongoDb4ProviderTest.assertProviderNamespace(..))" + " || " +
"execution(public * org.apache.logging.log4j.osgi.tests.AbstractLoadBundleTest.doOnBundlesAndVerifyState(..))" + " || " +
"execution(public * org.apache.logging.log4j.osgi.tests.CustomConfiguration.clearEvents(..))" + " || " +
"execution(public * org.apache.logging.log4j.osgi.tests.AbstractLoadBundleTest.testServiceLoader(..))" + " || " +
"execution(public * org.apache.logging.log4j.osgi.tests.AbstractLoadBundleTest.testLog4J12Fragement(..))" + " || " +
"execution(public * org.apache.logging.log4j.osgi.tests.AbstractLoadBundleTest.testClassNotFoundErrorLogger(..))" + " || " +
"execution(public * org.apache.logging.log4j.osgi.tests.AbstractLoadBundleTest.testApiCoreStartStopStartStop(..))" + " || " +
"execution(public * org.apache.logging.log4j.spring.boot.SpringLookup.setLoggerContext(..))" + " || " +
"execution(public * org.apache.logging.log4j.spring.boot.SpringProfileTest.testAppenderOut(..))" + " || " +
"execution(public * org.apache.logging.log4j.spring.boot.SpringProfileTest.registerSpringEnvironment(..))" + " || " +
"execution(public * org.apache.logging.log4j.spring.boot.SpringProfileTest.clearSpringEnvironment(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.ExceptionAwareTagSupport.setException(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.CatchingTagTest.verify(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.CatchingTag.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.DumpTag.setScope(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.EnterTagTest.verify(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.EntryTag.setDynamicAttribute(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.ExceptionAwareTagSupport.init(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.ExitTagTest.verify(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.ExitTag.setResult(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.IfEnabledTag.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.IfEnabledTag.setMarker(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggerAwareTagSupportTest.setUp(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggerAwareTagSupport.setLogger(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggerAwareTagSupport.release(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggingMessageTagSupportTest.setUp(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggingMessageTagSupport.setMessage(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggingMessageTagSupport.setBodyContent(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggingMessageTagSupportTest.verify(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggingMessageTagSupport.setMarker(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LoggingMessageTagSupport.setDynamicAttribute(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LogTag.setLevel(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.LogTag.init(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.SetLoggerTag.setLogger(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.SetLoggerTag.setVar(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.SetLoggerTag.setFactory(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.SetLoggerTagTest.checkMessageFactory(..))" + " || " +
"execution(public * org.apache.logging.log4j.taglib.SetLoggerTag.setScope(..))" + " || " +
"execution(public * org.apache.logging.log4j.Logger.exit(..))")
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
package net.hasor.dataql.kernel;

import java.util.Collections;
import java.util.Map;
import net.hasor.dataql.compiler.qil.QIL;
import net.hasor.dataql.domain.DataModel;
import net.hasor.dataql.domain.Hints;
import net.hasor.dataql.kernel.inset.OpcodesPool;
import net.hasor.dataql.kernel.mem.DataHeap;
import net.hasor.dataql.kernel.mem.DataStack;
import net.hasor.dataql.kernel.mem.EnvStack;
import net.hasor.dataql.kernel.mem.ExitType;

/**
 * 查询执行器，与 QIL 一对一绑定。
 */
public class QueryExecutor {
    private final QIL    qil;
    private final Finder finder;

    public QueryExecutor(QIL qil, Finder finder) {
        this.qil = qil;
        this.finder = finder;
    }

    public QueryResultImpl execute(Hints hints, Map<String, Object> shareVarMap, CustomizeScope customize) throws QueryRuntimeException {
        InstSequence instSequence = new InstSequence(0, this.qil);

        if (customize == null) {
            customize = symbol -> Collections.emptyMap();
        }
        InsetProcessContext processContext = new InsetProcessContext(customize, this.finder);
        processContext.currentHints().setHints(hints);

        DataStack dataStack = new DataStack();
        DataHeap dataHeap = new DataHeap();
        EnvStack envStack = new EnvStack();
        this.qil.getCompilerVar().forEach((varName, varLocalIdx) -> {
            Object varVal = shareVarMap != null ? shareVarMap.get(varName) : null;
            dataHeap.saveData(varLocalIdx, varVal);
        });

        OpcodesPool opcodesPool = OpcodesPool.defaultOpcodesPool();
        while (instSequence.hasNext()) {
            opcodesPool.doWork(instSequence, dataHeap, dataStack, envStack, processContext);
            instSequence.doNext(1);
        }

        ExitType exitType = (dataStack.getExitType() == null) ? ExitType.Return : dataStack.getExitType();
        int resultCode = dataStack.getResultCode();
        DataModel result = dataStack.getResult();
        long executionTime = processContext.executionTime();
        return new QueryResultImpl(exitType, resultCode, result, executionTime);
    }
}

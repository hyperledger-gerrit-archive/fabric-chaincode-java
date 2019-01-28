package contract;

import io.swagger.v3.oas.annotations.info.Info;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Init;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.Chaincode;
import org.hyperledger.fabric.shim.ResponseUtils;

import java.util.List;

@Contract(
        namespace = "samplecontract",
        info = @Info(

        )
)
public class SampleContract implements ContractInterface {
    static public int beforeInvoked = 0;
    static public int afterInvoked = 0;
    static public int doWorkInvoked = 0;
    static public int t1Invoked = 0;
    static public int i1Invoked = 0;

    @Init
    public Chaincode.Response i1() {
        i1Invoked++;
        return ResponseUtils.newSuccessResponse("Init done");
    }

    @Transaction
    public Chaincode.Response t1(String arg1) {
        t1Invoked++;
        Context context = getContext();
        List<String> args = context.getStringArgs();
        doSomeWork();
        return ResponseUtils.newSuccessResponse(args.get(1));
    }

    @Override
    public void beforeTransaction() {
        beforeInvoked++;
    }

    @Override
    public void afterTransaction() {
        afterInvoked++;
    }
    private void doSomeWork() {
        doWorkInvoked++;
    }
}

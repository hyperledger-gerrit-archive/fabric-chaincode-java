package contract;

import io.swagger.v3.oas.annotations.info.Info;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;

import java.util.List;

@Contract(
        namespace = "samplecontract",
        info = @Info(

        )
)
public class SampleContract implements ContractInterface {
    String t1(String arg1) {
        Context context = getContext();
        List<byte[]> args = context.getArgs();
    }
}

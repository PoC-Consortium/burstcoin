package brs.http;

import static brs.http.common.Parameters.ACCOUNT_ID_PARAMETER;
import static brs.http.common.Parameters.NONCE_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;

import brs.Account;
import brs.Blockchain;
import brs.Generator;
import brs.crypto.Crypto;
import brs.services.AccountService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;


public final class SubmitNonce extends APIServlet.APIRequestHandler {

  private final AccountService accountService;
  private final Blockchain blockchain;
  private final Generator generator;

  SubmitNonce(AccountService accountService, Blockchain blockchain, Generator generator) {
    super(new APITag[] {APITag.MINING}, SECRET_PHRASE_PARAMETER, NONCE_PARAMETER, ACCOUNT_ID_PARAMETER);

    this.accountService = accountService;
    this.blockchain = blockchain;
    this.generator = generator;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    String secret = req.getParameter(SECRET_PHRASE_PARAMETER);
    long nonce = Convert.parseUnsignedLong(req.getParameter(NONCE_PARAMETER));
		
    String accountId = req.getParameter(ACCOUNT_ID_PARAMETER);
		
    JSONObject response = new JSONObject();
		
    if(secret == null) {
      response.put("result", "Missing Passphrase");
      return response;
    }
		
    byte[] secretPublicKey = Crypto.getPublicKey(secret);
    Account secretAccount = accountService.getAccount(secretPublicKey);
    if(secretAccount != null) {
      Account genAccount;
      if(accountId != null) {
        genAccount = accountService.getAccount(Convert.parseAccountId(accountId));
      }
      else {
        genAccount = secretAccount;
      }
			
      if(genAccount != null) {
        Account.RewardRecipientAssignment assignment = accountService.getRewardRecipientAssignment(genAccount);
        Long rewardId;
        if(assignment == null) {
          rewardId = genAccount.getId();
        }
        else if(assignment.getFromHeight() > blockchain.getLastBlock().getHeight() + 1) {
          rewardId = assignment.getPrevRecipientId();
        }
        else {
          rewardId = assignment.getRecipientId();
        }
        if(rewardId != secretAccount.getId()) {
          response.put("result", "Passphrase does not match reward recipient");
          return response;
        }
      }
      else {
        response.put("result", "Passphrase is for a different account");
        return response;
      }
    }
		
    Generator.GeneratorState generatorState = null;
    if(accountId == null || secretAccount == null) {
      generatorState = generator.addNonce(secret, nonce);
    }
    else {
      Account genAccount = accountService.getAccount(Convert.parseUnsignedLong(accountId));
      if(genAccount == null || genAccount.getPublicKey() == null) {
        response.put("result", "Passthrough mining requires public key in blockchain");
      }
      else {
        byte[] publicKey = genAccount.getPublicKey();
        generatorState = generator.addNonce(secret, nonce, publicKey);
      }
    }
		
    if(generatorState == null) {
      response.put("result", "failed to create generator");
      return response;
    }
		
    //response.put("result", "deadline: " + generator.getDeadline());
    response.put("result", "success");
    response.put("deadline", generatorState.getDeadline());
		
    return response;
  }
	
  @Override
  boolean requirePost() {
    return true;
  }
}

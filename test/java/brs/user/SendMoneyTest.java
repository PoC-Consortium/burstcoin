package brs.user;

import static brs.Constants.ONE_BURST;
import static brs.Constants.RESPONSE;
import static brs.common.TestConstants.FEE;
import static brs.common.TestConstants.TEST_ACCOUNT_ID;
import static brs.common.TestConstants.TEST_SECRET_PHRASE;
import static brs.http.common.Parameters.AMOUNT_BURST_PARAMETER;
import static brs.http.common.Parameters.DEADLINE_PARAMETER;
import static brs.http.common.Parameters.FEE_BURST_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.user.JSONResponses.NOTIFY_OF_ACCEPTED_TRANSACTION;
import static brs.user.SendMoney.AMOUNT_MIN_MESSAGE;
import static brs.user.SendMoney.INCORRECT_FIELD_MESSAGE;
import static brs.user.SendMoney.INCORRECT_TRANSACTION_RESPONSE;
import static brs.user.SendMoney.INSUFFICIENT_FUNDS_MESSAGE;
import static brs.user.SendMoney.INVALID_FEE_MESSAGE;
import static brs.user.SendMoney.INVALID_DEADLINE_MESSAGE;
import static brs.user.SendMoney.WRONG_SECRET_MESSAGE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyShort;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Attachment;
import brs.Burst;
import brs.Transaction;
import brs.TransactionProcessor;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.json.simple.JSONObject;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Account.class, Burst.class, Class.class})
@SuppressStaticInitializationFor("brs.Burst")
public class SendMoneyTest {

    private HttpServletRequest requestMock;
    private SendMoney sendMoney;
    private User user;
    private String TRANSACTION_AMOUNT = "" + ONE_BURST;

    @Before
    public void init() {
        requestMock = mock(HttpServletRequest.class);
        sendMoney = SendMoney.instance;

        //Initialize User and Request Parameters with valid values
        user = new User(TEST_ACCOUNT_ID);
        user.unlockAccount(TEST_SECRET_PHRASE);

        when(requestMock.getParameter(eq(RECIPIENT_PARAMETER))).thenReturn("123");
        when(requestMock.getParameter(eq(AMOUNT_BURST_PARAMETER))).thenReturn(TRANSACTION_AMOUNT);
        when(requestMock.getParameter(eq(FEE_BURST_PARAMETER))).thenReturn(FEE);
        when(requestMock.getParameter(eq(DEADLINE_PARAMETER))).thenReturn("10");
        when(requestMock.getParameter(eq(SECRET_PHRASE_PARAMETER))).thenReturn(TEST_SECRET_PHRASE);
    }

    @Test
    public void nullSecret_Test() throws Exception {
        user.lockAccount();

        assertEquals(sendMoney.processRequest(requestMock, user), null);
    }

    @Test
    public void invalidRecipient_Test() throws Exception {
        when(requestMock.getParameter(eq(RECIPIENT_PARAMETER))).thenReturn(null);

        final JSONObject result = (JSONObject)sendMoney.processRequest(requestMock, user);

        assertEquals(result.get(RESPONSE), INCORRECT_TRANSACTION_RESPONSE);
        assertEquals(result.get(MESSAGE_PARAMETER), INCORRECT_FIELD_MESSAGE);
    }

    @Test
    public void invalidSecret_Test() throws Exception {
        when(requestMock.getParameter(eq(SECRET_PHRASE_PARAMETER))).thenReturn("InvalidSecret");

        final JSONObject result = (JSONObject)sendMoney.processRequest(requestMock, user);

        assertEquals(result.get(RESPONSE), INCORRECT_TRANSACTION_RESPONSE);
        assertEquals(result.get(MESSAGE_PARAMETER), WRONG_SECRET_MESSAGE);
    }

    @Test
    public void invalidNQTAmount_Test() throws Exception {
        when(requestMock.getParameter(eq(AMOUNT_BURST_PARAMETER))).thenReturn("0");

        final JSONObject result = (JSONObject)sendMoney.processRequest(requestMock, user);

        assertEquals(result.get(RESPONSE), INCORRECT_TRANSACTION_RESPONSE);
        assertEquals(result.get(MESSAGE_PARAMETER), AMOUNT_MIN_MESSAGE);
    }

    @Test
    public void invalidFeeAmount_Test() throws Exception {
        when(requestMock.getParameter(eq(FEE_BURST_PARAMETER))).thenReturn("0");

        final JSONObject result = (JSONObject)sendMoney.processRequest(requestMock, user);

        assertEquals(result.get(RESPONSE), INCORRECT_TRANSACTION_RESPONSE);
        assertEquals(result.get(MESSAGE_PARAMETER), INVALID_FEE_MESSAGE);
    }

    @Test
    public void invalidDeadline_Test() throws Exception {
        when(requestMock.getParameter(eq(DEADLINE_PARAMETER))).thenReturn("0");

        final JSONObject result = (JSONObject)sendMoney.processRequest(requestMock, user);

        assertEquals(result.get(RESPONSE), INCORRECT_TRANSACTION_RESPONSE);
        assertEquals(result.get(MESSAGE_PARAMETER), INVALID_DEADLINE_MESSAGE);
    }

    @Test
    public void insufficientFunds_Test() throws Exception {
        PowerMockito.mockStatic(Account.class);
        Account mockAccount = mock(Account.class);
        when(mockAccount.getUnconfirmedBalanceNQT()).thenReturn(0L);
        when(Account.getAccount(any(byte[].class))).thenReturn(mockAccount);

        final JSONObject result = (JSONObject)sendMoney.processRequest(requestMock, user);

        assertEquals(result.get(RESPONSE), INCORRECT_TRANSACTION_RESPONSE);
        assertEquals(result.get(MESSAGE_PARAMETER), INSUFFICIENT_FUNDS_MESSAGE);
    }

    @Test
    public void successfulTransaction_Test() throws Exception {
        Account mockAccount = mock(Account.class);
        long burstPlusFee = Convert.parseNXT(TRANSACTION_AMOUNT) + Convert.parseNXT(FEE);
        when(mockAccount.getUnconfirmedBalanceNQT()).thenReturn(burstPlusFee + 1);

        PowerMockito.mockStatic(Account.class);
        when(Account.getAccount(any(byte[].class))).thenReturn(mockAccount);

        TransactionProcessor transactionProcessorMock = mock(TransactionProcessor.class);
        Transaction.Builder mockBuilder = mock(Transaction.Builder.class);
        when(mockBuilder.recipientId((anyLong()))).thenReturn(mockBuilder);
        when(transactionProcessorMock.newTransactionBuilder(any(byte[].class), anyLong(), anyLong(), anyShort(), any(Attachment.class))).thenReturn(mockBuilder);

        final Transaction transactionMock = mock(Transaction.class);
        when(mockBuilder.build()).thenReturn(transactionMock);

        PowerMockito.mockStatic(Burst.class);
        when(Burst.getTransactionProcessor()).thenReturn(transactionProcessorMock);

        assertEquals(sendMoney.processRequest(requestMock, user), NOTIFY_OF_ACCEPTED_TRANSACTION);
    }
}

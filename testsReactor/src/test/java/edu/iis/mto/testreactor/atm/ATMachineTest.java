package edu.iis.mto.testreactor.atm;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import edu.iis.mto.testreactor.atm.bank.AccountException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationException;
import edu.iis.mto.testreactor.atm.bank.AuthorizationToken;
import edu.iis.mto.testreactor.atm.bank.Bank;

@ExtendWith(MockitoExtension.class)
public class ATMachineTest {

    @Mock
    private Bank bank;

    private ATMachine atMachine;

    private Currency currency;

    private PinCode pinCode;

    private Card card;

    private Money amount;

    @BeforeEach
    public void setUp() {
        currency = Money.DEFAULT_CURRENCY;
        atMachine = new ATMachine(bank, currency);
    }

    @Test
    public void withdrawShouldReturnProperBanknotes() throws ATMOperationException, AuthorizationException {
        pinCode = PinCode.createPIN(1, 2, 3, 4);
        card = Card.create("card1");
        amount = new Money(70, Money.DEFAULT_CURRENCY);

        List<BanknotesPack> banknotesPack = new ArrayList<>();
        banknotesPack.add(BanknotesPack.create(3, Banknote.PL_50));
        banknotesPack.add(BanknotesPack.create(2, Banknote.PL_20));
        banknotesPack.add(BanknotesPack.create(4, Banknote.PL_10));
        MoneyDeposit deposit = MoneyDeposit.create(currency, banknotesPack);
        atMachine.setDeposit(deposit);

        AuthorizationToken token = AuthorizationToken.create("token");
        Mockito.when(bank.autorize(pinCode.getPIN(), card.getNumber()))
               .thenReturn(token);

        Withdrawal withdrawal = atMachine.withdraw(pinCode, card, amount);
        List<Banknote> expected = Arrays.asList(Banknote.PL_50, Banknote.PL_20);
        List<Banknote> actual = withdrawal.getBanknotes();
        assertThat(actual, is(expected));
    }

    @Test
    public void withdrawShouldThrowATMOperationExceptionWhenAuthorizeFailed() throws ATMOperationException, AuthorizationException {
        pinCode = PinCode.createPIN(1, 2, 3, 4);
        card = Card.create("card1");
        amount = new Money(70, Money.DEFAULT_CURRENCY);

        Mockito.doThrow(AuthorizationException.class)
               .when(bank)
               .autorize(pinCode.getPIN(), card.getNumber());

        try {
            atMachine.withdraw(pinCode, card, amount);
        } catch (ATMOperationException e) {
            assertTrue(e.getErrorCode()
                        .equals(ErrorCode.AHTHORIZATION));
        }
    }

    @Test
    public void withdrawShouldThrowATMOperationExceptionWhenDepositEmpty() throws ATMOperationException, AuthorizationException {
        pinCode = PinCode.createPIN(1, 2, 3, 4);
        card = Card.create("card1");
        amount = new Money(70, Money.DEFAULT_CURRENCY);

        List<BanknotesPack> banknotesPack = Collections.emptyList();
        MoneyDeposit deposit = MoneyDeposit.create(currency, banknotesPack);
        atMachine.setDeposit(deposit);

        AuthorizationToken token = AuthorizationToken.create("token");
        Mockito.when(bank.autorize(pinCode.getPIN(), card.getNumber()))
               .thenReturn(token);

        try {
            atMachine.withdraw(pinCode, card, amount);
        } catch (ATMOperationException e) {
            assertTrue(e.getErrorCode()
                        .equals(ErrorCode.WRONG_AMOUNT));
        }
    }

    @Test
    public void withdrawShouldThrowATMOperationExceptionWhenBankTransactionFailed()
            throws ATMOperationException, AuthorizationException, AccountException {
        pinCode = PinCode.createPIN(1, 2, 3, 4);
        card = Card.create("card1");
        amount = new Money(70, Money.DEFAULT_CURRENCY);

        List<BanknotesPack> banknotesPack = new ArrayList<>();
        banknotesPack.add(BanknotesPack.create(3, Banknote.PL_50));
        banknotesPack.add(BanknotesPack.create(2, Banknote.PL_20));
        banknotesPack.add(BanknotesPack.create(4, Banknote.PL_10));
        MoneyDeposit deposit = MoneyDeposit.create(currency, banknotesPack);
        atMachine.setDeposit(deposit);

        AuthorizationToken token = AuthorizationToken.create("token");
        Mockito.when(bank.autorize(pinCode.getPIN(), card.getNumber()))
               .thenReturn(token);

        Mockito.doThrow(AccountException.class)
               .when(bank)
               .charge(Mockito.any(), Mockito.any());

        try {
            atMachine.withdraw(pinCode, card, amount);
        } catch (ATMOperationException e) {
            assertTrue(e.getErrorCode()
                        .equals(ErrorCode.NO_FUNDS_ON_ACCOUNT));
        }
    }

    @Test
    public void withdrawShouldThrowATMOperationExceptionWhenWrongCurrency()
            throws ATMOperationException, AuthorizationException, AccountException {
        pinCode = PinCode.createPIN(1, 2, 3, 4);
        card = Card.create("card1");
        amount = new Money(70, Currency.getInstance("USD"));

        try {
            atMachine.withdraw(pinCode, card, amount);
        } catch (ATMOperationException e) {
            assertTrue(e.getErrorCode()
                        .equals(ErrorCode.WRONG_CURRENCY));
        }
    }

    @Test
    public void withdrawShouldCallProperMethodsInOrder() throws ATMOperationException, AuthorizationException, AccountException {
        pinCode = PinCode.createPIN(1, 2, 3, 4);
        card = Card.create("card1");
        amount = new Money(70, Money.DEFAULT_CURRENCY);

        List<BanknotesPack> banknotesPack = new ArrayList<>();
        banknotesPack.add(BanknotesPack.create(3, Banknote.PL_50));
        banknotesPack.add(BanknotesPack.create(2, Banknote.PL_20));
        banknotesPack.add(BanknotesPack.create(4, Banknote.PL_10));
        MoneyDeposit deposit = MoneyDeposit.create(currency, banknotesPack);
        atMachine.setDeposit(deposit);

        AuthorizationToken token = AuthorizationToken.create("token");
        Mockito.when(bank.autorize(pinCode.getPIN(), card.getNumber()))
               .thenReturn(token);

        atMachine.withdraw(pinCode, card, amount);
        InOrder callOrder = Mockito.inOrder(bank);
        callOrder.verify(bank)
                 .autorize(Mockito.any(), Mockito.any());
        callOrder.verify(bank)
                 .charge(Mockito.any(), Mockito.any());
    }

}

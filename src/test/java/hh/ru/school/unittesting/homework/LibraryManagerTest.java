package hh.ru.school.unittesting.homework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.school.unittesting.homework.LibraryManager;
import ru.hh.school.unittesting.homework.NotificationService;
import ru.hh.school.unittesting.homework.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LibraryManagerTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private UserService userService;
    @InjectMocks
    private LibraryManager libraryManager;

    @Test
    void testGetAvailableCopiesOfUnknownBookReturnsZero() {
        int availableCopies = libraryManager.getAvailableCopies("unknownBookId");

        assertEquals(0, availableCopies);
    }

    @Test
    void testAddMoreCopiesToExistingBookIncreasesAvailableCopies() {
        libraryManager.addBook("testBookId", 3);
        libraryManager.addBook("testBookId", 2);
        int availableCopies = libraryManager.getAvailableCopies("testBookId");

        assertEquals(5, availableCopies);
    }

    @Test
    void testAddCopiesToNewBookAddsToInventory() {
        libraryManager.addBook("newBookId", 7);
        int availableCopies = libraryManager.getAvailableCopies("newBookId");

        assertEquals(7, availableCopies);
    }

    @Test
    void testBorrowBookForInactiveUserReturnsFalse() {
        when(userService.isUserActive("testUserId")).thenReturn(false);

        boolean borrowResult = libraryManager.borrowBook("testBookId", "testUserId");

        verify(notificationService).notifyUser("testUserId", "Your account is not active.");
        assertFalse(borrowResult);
    }

    @Test
    void testBorrowUnknownBookReturnsFalse() {
        when(userService.isUserActive("testUserId")).thenReturn(true);

        boolean borrowResult = libraryManager.borrowBook("unknownBookId", "testUserId");

        assertFalse(borrowResult);
    }

    @Test
    void testBorrowBookWithNoAvailableCopiesReturnsFalse() {
        when(userService.isUserActive("testUserId")).thenReturn(true);

        libraryManager.addBook("testBookId", 0);
        boolean borrowResult = libraryManager.borrowBook("testBookId", "testUserId");

        assertFalse(borrowResult);
    }

    @Test
    void testBorrowBookDecreasesAvailableCopies() {
        when(userService.isUserActive("testUserId")).thenReturn(true);

        libraryManager.addBook("testBookId", 3);
        boolean borrowResult = libraryManager.borrowBook("testBookId", "testUserId");

        verify(notificationService).notifyUser("testUserId", "You have borrowed the book: testBookId");

        int availableCopies = libraryManager.getAvailableCopies("testBookId");

        assertTrue(borrowResult);
        assertEquals(2, availableCopies);
    }

    @Test
    void testReturnUnknownBookReturnsFalse() {
        boolean returnResult = libraryManager.returnBook("unknownBookId", "testUserId");

        assertFalse(returnResult);
    }

    @Test
    void testReturnBookBorrowedByAnotherUserReturnsFalse() {
        when(userService.isUserActive("anotherUserId")).thenReturn(true);

        libraryManager.addBook("testBookId", 1);
        libraryManager.borrowBook("testBookId", "anotherUserId");
        boolean returnResult = libraryManager.returnBook("testBookId", "testUserId");

        assertFalse(returnResult);
    }

    @Test
    void testReturnBookIncreasesAvailableCopies() {
        when(userService.isUserActive("testUserId")).thenReturn(true);

        libraryManager.addBook("testBookId", 3);
        libraryManager.borrowBook("testBookId", "testUserId");
        boolean returnResult = libraryManager.returnBook("testBookId", "testUserId");

        verify(notificationService).notifyUser("testUserId", "You have borrowed the book: testBookId");
        verify(notificationService).notifyUser("testUserId", "You have returned the book: testBookId");

        int availableCopies = libraryManager.getAvailableCopies("testBookId");

        assertTrue(returnResult);
        assertEquals(3, availableCopies);
    }

    @Test
    void testCalculateDynamicLateFeeWithInvalidOverdueDaysValueThrowsException() {
        var exception = assertThrows(
                IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-5, false, false)
        );
        assertEquals("Overdue days cannot be negative.", exception.getMessage());
    }

    /*
        Мне кажется, так как коэффициенты в определении метода calculateDynamicLateFee
        вынесены в виде констант, то предполагается, что они изменяемые. Следовательно,
        хорошо было бы построить тест так, чтобы при изменении значения констант он не ломался.
        Но я не знаю, как это сделать :( Про mockito посмотрел, вроде он так не умеет.
     */

    @ParameterizedTest
    @CsvSource({
            "2, false, false, 1",
            "6, false, true, 2.4",
            "13, true, false, 9.75",
            "11, true, true, 6.6"
    })
    void testCalculateDynamicLateFee(
        int overdueDays,
        boolean isBestseller,
        boolean isPremiumMember,
        double expectedFee
    ) {
        double fee = libraryManager.calculateDynamicLateFee(
            overdueDays,
            isBestseller,
            isPremiumMember
        );

        assertEquals(expectedFee, fee);
    }

    @ParameterizedTest
    @CsvSource({
            "0, false, false, 0",
            "0, false, true, 0",
            "0, true, false, 0",
            "0, true, true, 0"
    })
    void testCalculateDynamicLateFeeWithZeroDaysReturnsZero(
            int overdueDays,
            boolean isBestseller,
            boolean isPremiumMember,
            double expectedFee
    ) {
        double fee = libraryManager.calculateDynamicLateFee(
                overdueDays,
                isBestseller,
                isPremiumMember
        );
        assertEquals(expectedFee, fee);
    }
}

package net.rideshare_ptc;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.*;

import android.widget.DatePicker;
import android.widget.TimePicker;

import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import dagger.hilt.android.testing.HiltAndroidTest;

/**
 * Integration tests for Ride Posting features in the app
 */
@RunWith(AndroidJUnit4.class)
public class RequestARideTests {
    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    @Before
    public void setUp() throws Exception {
        login();
    }

    /**
     * Logs into the app.
     * Runs before all tests to ensure that they function properly.
     * Assume that tests in LoginActivityTests.java are all passing.
     */
    private void login() throws IOException {
        //type in the username
        onView(withId(R.id.txtInputLoginEM)).perform(typeText("RSC14@students.ptcollege.edu"), ViewActions.closeSoftKeyboard());

        //type in the password
        onView(withId(R.id.txtInputLoginPW)).perform(typeText("password1"), ViewActions.closeSoftKeyboard());

        //click submit
        onView(withId(R.id.btnSubmitUser)).perform(click());
    }

    /**
     * Navigates to the Request A Ride page from the home page
     * Note: Will cause test/execution failure if this method is
     * not ran from the home page.
     */
    private void navigateToRequestARide(){
        onView(withId(R.id.btnMenuReq)).perform(click());
    }

    /**
     * Picks the date and time from the date-time picker button
     * on the Request A Ride Page.
     *
     * Picks a date of 1-1-2000 and a time of 12:30 PM
     */
    private void pickDateAndTime(){
        //click on the calendar button
        onView(withId(R.id.btnCalendar)).perform(click());

        //Find the calendar and set a date
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName()))).perform(PickerActions.setDate(2000, 1, 1));
        onView(withText("OK")).perform(click());

        //Find the Clock and set a time
        onView(withClassName(Matchers.equalTo(TimePicker.class.getName()))).perform(PickerActions.setTime(12, 30));
        onView(withText("OK")).perform(click());
    }

    /**
     * Navigates back to the home page from the Request
     * A Ride page. Should be ran after tests that do not actually
     * perform operations on the Request A Ride Page.
     *
     * I'm using this just to be safe that
     * all tests function as intended.
     */
    private void leaveRequestARide(){
        onView(withId(R.id.btnReqRideRetMenu)).perform(click());
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void Test_RequestARide_AllElementsAreDisplayed(){
        navigateToRequestARide(); //navigate to request a ride from home page
        onView(withId(R.id.btnCalendar)).check(matches(isDisplayed()));
        onView(withId(R.id.inptReqPickUpLoc)).check(matches(isDisplayed()));
        onView(withId(R.id.inptReqDestLoc)).check(matches(isDisplayed()));
        onView(withId(R.id.ReqcheckBoxSmoking)).check(matches(isDisplayed()));
        onView(withId(R.id.ReqcheckBoxTalking)).check(matches(isDisplayed()));
        onView(withId(R.id.ReqcheckBoxHasCarseat)).check(matches(isDisplayed()));
        onView(withId(R.id.ReqcheckBoxEating)).check(matches(isDisplayed()));
        onView(withId(R.id.btnReqRideRetMenu)).check(matches(isDisplayed()));
        onView(withId(R.id.btnReqARide)).check(matches(isDisplayed()));
        leaveRequestARide();
    }

    /**
     * Posts a ride to the database using the app's Request a Ride
     * page. Then checks the database to ensure that the ride was posted
     * with the correct information.
     */
    @Test
    public void Test_RequestARide_PostsRides(){
        navigateToRequestARide();
        pickDateAndTime();

        //Pick up location and destination use the examples from the sample distance matrix api request
        //Distance Expected: 228 (mi)
        //Duration Expected: 14220 (seconds)
        onView(withId(R.id.inptReqPickUpLoc)).perform(typeText("New York, NY"), ViewActions.closeSoftKeyboard());
        onView(withId(R.id.inptReqDestLoc)).perform(typeText("Washington, DC"), ViewActions.closeSoftKeyboard());

        //click submit
        onView(withId(R.id.btnReqARide)).perform(click());

        //Wait
        MockClock clock = new MockClock();
        clock.sleep(2000); //wait 2 seconds

        //Assert that the entry is in the database
        boolean foundInDatabase = checkDatabaseForNewEntry();
        assertTrue("Ride was not found in the database! Ride not posted", foundInDatabase);
        deleteEntriesFromDatabase();
        
    }

    /**
     * Checks the database for the expected entry in the rides table
     * @return true if the expected entry was found, false if it wasn't
     */
    private boolean checkDatabaseForNewEntry() {
        try{
            Class.forName("com.microsoft.sqlserver");
        }catch (ClassNotFoundException e){
            System.out.println("Driver could not be set up properly");
        }
        Connection conn = null;
        boolean rideFound = false;
        System.out.println("Starting thing: " );
        try {
            conn = DriverManager.getConnection("jdbc:sqlserver://jdsteltz.database.windows.net:1433;database=EnterpriseApps;user=jdsteltz@jdsteltz;password=Dawson226!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;");
            System.out.println("We are in the thing");
            if (conn != null) {
                System.out.println("Searching for entries by starting location");
                Statement searchQuery = conn.createStatement();
                String SQL = "select * from dbo.ride where rideDate = '1-1-2000 12:30'";
                ResultSet results = searchQuery.executeQuery(SQL);

                if (results.next()) //if we found anything
                {
                    //return true; //results found, return true
                    rideFound = true;
                } else {
                    //return false; //nothing was found, return false
                }
            }
        } catch (SQLException e) {
            System.out.println("SQL Exception thrown, check 'checkDatabaseForNewEntry' function");
            e.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    //ex.printStackTrace();
                }
            }
            return rideFound;
        }
    }

    /**
     * Deletes a ride by the expected test date (1-1-2000)
     */
    private static void deleteEntriesFromDatabase() {
        try{
            Class.forName("com.microsoft.sqlserver");
        }catch (ClassNotFoundException e){
            System.out.println("Driver could not be set up properly");
        }
        Connection conn = null;
        try {
            //Class.forName("com.microsoft.sqlserver");
            conn = DriverManager.getConnection("jdbc:sqlserver://jdsteltz.database.windows.net:1433;database=EnterpriseApps;user=jdsteltz@jdsteltz;password=Dawson226!;encrypt=true;trustServerCertificate=false;hostNameInCertificate=*.database.windows.net;loginTimeout=30;");
            if (conn != null) {
                System.out.println("\nDatabase connection successful\n");
                System.out.println(
                        "\nDeleting rides with a date of 1-1-2000 and ride time of 12:30 from the rides table\n");
                Statement deleteStmt = conn.createStatement();
                String SQL = "DELETE from dbo.Ride where rideDate = '1-1-2000 12:30';";
                int result = deleteStmt.executeUpdate(SQL);
                conn.commit();
                if (result == 0) {
                    System.out.println("\nRecord not found in database, check the rides table\n");
                } else {
                    System.out.println("\nRides deleted from database\n");
                }
            }
        } catch (SQLException ex) {
            System.out.println("\nConnection to database failed, delete rides with the expected test date (1-1-2000) from the database manually");
            ex.printStackTrace();
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    //ex.printStackTrace();
                }
            }
        }
    }
}
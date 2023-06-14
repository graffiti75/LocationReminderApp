package com.udacity.project4

import android.app.Application
import android.os.Build
import android.view.View
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito

/**
 * END TO END test to black box test the app.
 *
 * Source:
 * https://medium.com/koin-developers/unboxing-koin-2-1-7f1133ebb790
 */
@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
// Extended Koin Test - embed autoclose @After method to close Koin after every test
class RemindersActivityTest : KoinTest {

	// Executes each task synchronously using Architecture Components.
	@get:Rule
	var instantExecutorRule = InstantTaskExecutorRule()

	@get:Rule
	var activityScenarioRule = ActivityScenarioRule(RemindersActivity::class.java)

	/**
	 * https://stackoverflow.com/a/45320863
	 */
//	@get:Rule
//	var finePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
//	@get:Rule
//	var coarsePermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_COARSE_LOCATION)
//	@get:Rule
//	var backgroundPermissionRule = GrantPermissionRule.grant(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)

	// Set the main coroutines dispatcher for unit testing.
//	@ExperimentalCoroutinesApi
//	@get:Rule
//	var mainCoroutineRule = MainCoroutineRule()

	private lateinit var decorView: View

	// An Idling Resource that waits for Data Binding to have no pending bindings.
	private val dataBindingIdlingResource = DataBindingIdlingResource()

//	private lateinit var viewModel: SaveReminderViewModel
	private lateinit var repository: ReminderDataSource
	private lateinit var appContext: Application

	/**
	 * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
	 * At this step we will initialize Koin related code to be able to use it in our testing.
	 */
	@Before
	fun init() {
		// Stop the original app koin.
		stopKoin()
		appContext = getApplicationContext()

		val myModule = module {
			viewModel { RemindersListViewModel(appContext, get() as ReminderDataSource) }
			single { SaveReminderViewModel(appContext, get() as ReminderDataSource) }
			single { RemindersLocalRepository(get()) as ReminderDataSource }
			single { LocalDB.createRemindersDao(appContext) }
		}

		// Declare a new koin module.
		startKoin { modules(listOf(myModule)) }

		// Get our real repository.
		repository = get()

		// Clear the data to start fresh.
		runBlocking {
			repository.deleteAllReminders()
		}

		activityScenarioRule.scenario.onActivity { activity ->
			decorView = activity.window.decorView
		}
	}

	/**
	 * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
	 * are not scheduled in the main Looper (for example when executed on a different thread).
	 */
	@Before
	fun registerIdlingResource() {
		IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
		IdlingRegistry.getInstance().register(dataBindingIdlingResource)
	}

	/**
	 * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
	 */
	@After
	fun unregisterIdlingResource() {
		IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
		IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
	}

	@Test
	fun clickingOnFloatingActionButton_checkIfFieldsAreDisplayed() = runBlocking {
		val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityScenario)

		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
		onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
		onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))

		activityScenario.close()
	}

	@Test
	fun addingReminderWithoutTitle_checkIfPreventMessageIsShowed() = runBlocking {
		val activityActivityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityActivityScenario)

		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.saveReminder)).perform(click())
		onView(withText("Please enter title")).check(matches(isDisplayed()))

		activityActivityScenario.close()
	}

	/**
	 * To avoid flakiness, we highly recommend that you turn off system animations on the virtual or
	 * physical devices used for testing. On your device, under Settings > Developer options,
	 * disable the following 3 settings:
	 *
	 * - Window animation scale
	 * - Transition animation scale
	 * - Animator duration scale
	 *
	 * https://developer.android.com/training/testing/espresso/setup#set-up-environment
	 */
	/**
	 * Toast Assertions doesn't working in versions below 30.
	 *
	 * https://github.com/android/android-test/issues/803
	 */
	@Test
	fun addingReminder_verifyIfToastIsCalled() = runTest {
		// Verifying Exception - Toast Assertions doesn't work in versions above 30.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			throw Exception("Toast Assertions doesn't work in versions above 30.")
		}

		// Setup.
		val typingTitle = "Title espresso"
		val typingDescription = "Description espresso"
		val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
		dataBindingIdlingResource.monitorActivity(activityScenario)

		// Set Main dispatcher to not run coroutines eagerly, for just this one test.
//		val dispatcher = StandardTestDispatcher()
//		Dispatchers.setMain(dispatcher)

		val mock = Mockito.mock(ReminderDataSource::class.java)
		val viewModel = SaveReminderViewModel(appContext, mock)

		// GIVEN - Clicking to add a new task on Save Reminder screen.
		onView(withId(R.id.addReminderFAB)).perform(click())
		onView(withId(R.id.reminderTitle)).perform(ViewActions.typeText(typingTitle))
		onView(withId(R.id.reminderDescription)).perform(ViewActions.typeText(typingDescription))
		closeSoftKeyboard()

		(appContext as MyApp).testingMode = true
		onView(withId(R.id.selectLocation)).perform(click())

		// WHEN - Clicking in any position in the map, and saving the location
//		GlobalScope.launch(Dispatchers.Default) {
			onView(withId(R.id.map_fragment)).perform(click())
//		}

			// Execute pending coroutines actions.
//		dispatcher.scheduler.advanceUntilIdle() // https://kt.academy/article/cc-testing
//		advanceUntilIdle()

		// THEN - Save Reminder and check if Toast is displayed.
		val selectedLocation = viewModel.selectedPOI.value?.name
		onView(withId(R.id.saveReminder)).perform(click())
//		onView(withText(typingTitle)).check(matches(isDisplayed()))
//		onView(withText(typingDescription)).check(matches(isDisplayed()))
//		onView(withText(selectedLocation)).check(matches(isDisplayed()))

		onView(withText(R.string.reminder_saved)).inRoot(
			withDecorView(CoreMatchers.not(decorView))
		).check(matches(isDisplayed()))

		// Make sure the activity is closed before resetting the db:
		activityScenario.close()
	}
}
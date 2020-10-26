package com.moufee.purduemenus.ui.menu

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.work.*
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.moufee.purduemenus.BuildConfig
import com.moufee.purduemenus.R
import com.moufee.purduemenus.api.DownloadWorker
import com.moufee.purduemenus.databinding.ActivityMenuDatePickerTimeBinding
import com.moufee.purduemenus.preferences.KEY_PREF_DINING_COURT_ORDER
import com.moufee.purduemenus.preferences.KEY_PREF_SHOW_FAVORITE_COUNT
import com.moufee.purduemenus.preferences.KEY_PREF_USE_NIGHT_MODE
import com.moufee.purduemenus.repository.data.menus.DayMenu
import com.moufee.purduemenus.repository.data.menus.DiningCourtMeal
import com.moufee.purduemenus.ui.settings.SettingsActivity
import com.moufee.purduemenus.util.*
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.joda.time.LocalDate
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class MenuActivity : AppCompatActivity(), HasAndroidInjector {
    private lateinit var mBinding: ActivityMenuDatePickerTimeBinding
    private val mMenuPagerAdapter: MenuPagerAdapter = MenuPagerAdapter(this)
    private lateinit var mViewModel: DailyMenuViewModel
    private lateinit var networkListener: NetworkAvailabilityListener

    @Inject
    lateinit var mSharedPreferences: SharedPreferences

    @Inject
    lateinit var mDispatchingAndroidInjector: DispatchingAndroidInjector<Any>

    @Inject
    lateinit var mViewModelFactory: ViewModelProvider.Factory
    private val mSharedPreferenceChangeListener = OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_PREF_USE_NIGHT_MODE -> recreate()
            KEY_PREF_DINING_COURT_ORDER -> mViewModel.setDate(mViewModel.currentDate.value!!)
        }
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return mDispatchingAndroidInjector
    }

    private fun setListeners() {
        mViewModel.currentDate.observe(this,
                { dateTime: LocalDate ->
                    mBinding.dateTextView.text = DateTimeHelper.getFriendlyDateFormat(dateTime, Locale.getDefault(), applicationContext)
                })
        mViewModel.favoriteSet.observe(this, { strings: Set<String> -> mMenuPagerAdapter.setFavoritesSet(strings) })
        mViewModel.appPreferences.observe(this, { (_, showFavoriteCounts) -> mMenuPagerAdapter.setShowFavoriteCount(showFavoriteCounts) })
        mViewModel.sortedLocations.observe(this, { sorted: List<DiningCourtMeal> ->
            mMenuPagerAdapter.setMenus(sorted)
            Timber.d(sorted.toString())
        })
        mViewModel.dayMenu.observe(this, { resource: Resource<DayMenu> ->
            mBinding.menusResource = resource
            if (resource.status == Status.ERROR && resource.data == null) {
                Snackbar.make(mBinding.activityMenuCoordinatorLayout, getString(R.string.network_error_message), Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.d("onConfigurationChanged ")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        title = getString(R.string.app_name)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_menu_date_picker_time)
        mBinding.lifecycleOwner = this
        mViewModel = ViewModelProvider(this, mViewModelFactory).get(DailyMenuViewModel::class.java)
        mBinding.viewModel = mViewModel
        mSharedPreferences.registerOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener)
        val tabLayout = mBinding.menuTabLayout
        val toolbar = mBinding.mainToolbar
        setSupportActionBar(toolbar)
        mMenuPagerAdapter.setShowFavoriteCount(mSharedPreferences.getBoolean(KEY_PREF_SHOW_FAVORITE_COUNT, true))
        mBinding.menuViewPager.adapter = mMenuPagerAdapter
        TabLayoutMediator(tabLayout, mBinding.menuViewPager) { tab, position -> tab.text = mMenuPagerAdapter.getPageTitle(position) }.attach()
        setListeners()
        networkListener = NetworkAvailabilityListener(this, lifecycle) {
            mViewModel.reloadData()
        }
        displayChangelog()
        val workManager = WorkManager.getInstance(this)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build()
        val request = PeriodicWorkRequest.Builder(DownloadWorker::class.java, 1, TimeUnit.DAYS)
                .setConstraints(constraints)
                .build()
        workManager.enqueueUniquePeriodicWork("downloader", ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun displayChangelog() {
        val versionCode = BuildConfig.VERSION_CODE
        val hasShownMessage = mSharedPreferences.getBoolean(UPDATE_MESSAGE_KEY + versionCode, false)
        if (!hasShownMessage) {
            val changelogFragment: DialogFragment = ChangelogDialogFragment()
            changelogFragment.show(supportFragmentManager, "changelog")
            mSharedPreferences.edit().putBoolean(UPDATE_MESSAGE_KEY + versionCode, true).apply()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_options, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(SettingsActivity.getIntent(this))
                true
            }
            R.id.action_feedback -> {
                val emailIntent = Intent(Intent.ACTION_SENDTO)
                emailIntent.data = Uri.parse("mailto:") // only email apps should handle this
                emailIntent.putExtra(Intent.EXTRA_EMAIL, arrayOf("support@benferris.tech")) // recipients
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Purdue Menus Feedback (version ${BuildConfig.VERSION_NAME})")
                if (emailIntent.resolveActivity(packageManager) != null) {
                    startActivity(emailIntent)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(mSharedPreferenceChangeListener)
        super.onDestroy()
    }
}
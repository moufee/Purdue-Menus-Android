package com.moufee.purduemenus.di;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.franmontiel.persistentcookiejar.PersistentCookieJar;
import com.franmontiel.persistentcookiejar.cache.SetCookieCache;
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.moufee.purduemenus.api.FileCookiePersistor;
import com.moufee.purduemenus.api.LocalTimeTypeAdapter;
import com.moufee.purduemenus.api.LocalTimeTypeConverter;
import com.moufee.purduemenus.api.Webservice;
import com.moufee.purduemenus.db.AppDatabase;
import com.moufee.purduemenus.db.FavoriteDao;
import com.moufee.purduemenus.util.AppExecutors;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;

import org.joda.time.LocalTime;

import javax.inject.Singleton;

import androidx.room.Room;
import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

/**
 * Created by Ben on 28/07/2017.
 * The Dagger App Module, Provides dependencies for injection
 */

@Module(includes = {ViewModelModule.class})
class AppModule {

    @Singleton
    @Provides
    Webservice provideWebService(AppExecutors executors, Moshi moshi, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl("https://api.hfs.purdue.edu")
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .callbackExecutor(executors.diskIO())
                .build()
                .create(Webservice.class);
    }

    @Singleton
    @Provides
    Moshi provideMoshi() {
        return new Moshi.Builder()
                .add(new LocalTimeTypeAdapter())
                .add(new KotlinJsonAdapterFactory())
                .build();
    }

    @Singleton
    @Provides
    Gson provideGson() {
        return new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
                .registerTypeAdapter(LocalTime.class, new LocalTimeTypeConverter())
                .create();
    }

    @Singleton
    @Provides
    OkHttpClient provideHttpClient(FileCookiePersistor fileCookiePersistor, Context context) {
        //todo: restore cookies or not?
        return new OkHttpClient.Builder()
                .cookieJar(new PersistentCookieJar(new SetCookieCache(), new SharedPrefsCookiePersistor(context)))
                .build();
    }

    @Singleton
    @Provides
    SharedPreferences sharedPreferences(Application application) {
        return PreferenceManager.getDefaultSharedPreferences(application);
    }

    @Singleton
    @Provides
    Context provideContext(Application application) {
        return application.getApplicationContext();
    }

    @Singleton
    @Provides
    AppDatabase provideAppDatabase(Context applicationContext) {
        return Room.databaseBuilder(applicationContext, AppDatabase.class, "menus-database")
                .addMigrations(AppDatabase.MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build();
    }

    @Singleton
    @Provides
    FavoriteDao provideFavoriteDao(AppDatabase appDatabase) {
        return appDatabase.favoriteDao();
    }

}

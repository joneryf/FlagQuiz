package com.developer.jonery.flagquiz;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {

    //key for reading data from SharedPreferences
    public static final String CHOICES = "pref_numberOfChoices";
    public static final String REGIONS = "pref_regionsToInclude";

    //used to force portrait mode
    private boolean phoneDevice = true;
    //to see if preferences were changed
    private boolean preferencesChanged = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //set default for SharedPreferences, o false é boolean para set default novamente se for necessário,
        // é só criar uma variável quando usuário pedir para resetar
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        //determina tamanho da tela
        int screenSize = getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;

        //se layout é um tablet, phoneDevice se define false
        if(screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE ||
                screenSize == Configuration.SCREENLAYOUT_SIZE_XLARGE){
            phoneDevice = false;
        }

        //se estiver rodando em um telefone permite somente retrato
        if(phoneDevice){
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(preferencesChanged){
            //neste momento que as preferencias foram setadas, inicia o MainActivityfragment e começa o quiz
            MainActivityFragment quizFragment = (MainActivityFragment) getSupportFragmentManager().
                    findFragmentById(R.id.quizFragment);
            quizFragment.updateGuessRows(
                    PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.updateRegions(
                    PreferenceManager.getDefaultSharedPreferences(this));
            quizFragment.resetQuiz();
            preferencesChanged = false;

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        //verifica o dispositivo, se é phone ou tablet, e só abre menu para phone
        int orientation = getResources().getConfiguration().orientation;
        // Inflate the menu; this adds items to the action bar if it is present.
        //apenas abre o menu se for telefone ou tablet em formato retrato
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            return true;
        } else
            return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //Neste caso o menu tem somente um item, então não precisa verificar qual item foi
        //selecionado
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }
        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);

        return super.onOptionsItemSelected(item);
    }

    //listener pra verificar se preferencias foram modificadas
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener(){
                @Override
                public void onSharedPreferenceChanged(
                        SharedPreferences sharedPreferences, String key){
                    preferencesChanged = true;

                    MainActivityFragment quizFragment = (MainActivityFragment)
                            getSupportFragmentManager().findFragmentById(R.id.quizFragment);

                    if(key.equals(CHOICES)){
                        quizFragment.updateGuessRows(sharedPreferences);
                        quizFragment.resetQuiz();
                    } else if (key.equals(REGIONS)){
                        Set<String> regions = sharedPreferences.getStringSet(REGIONS, null);

                        if(regions != null && regions.size()>0){
                            quizFragment.updateRegions(sharedPreferences);
                            quizFragment.resetQuiz();
                        }
                        else {
                            //precisa selecionar no minimo uma regiao
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            regions.add(getString(R.string.default_region));
                            editor.putStringSet(REGIONS, regions);
                            editor.apply();

                            Toast.makeText(MainActivity.this, R.string.default_region_message, Toast.LENGTH_LONG).show();
                        }
                    }
                    Toast.makeText(MainActivity.this, R.string.restarting_quiz, Toast.LENGTH_LONG).show();
                }
            };


}

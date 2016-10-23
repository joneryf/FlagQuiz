package com.developer.jonery.flagquiz;

import android.animation.Animator;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    //string pra logar mensagens, pode ser pego o nome da classe tambem
    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList; //flag file names
    private List<String> quizCountriesList; //countries in current quiz
    private Set<String> regionsSet; //world regions in current quiz
    private String correctAnswer; //correct country for the flag
    private int totalGuesses; //number of guesses made
    private int correctAnswers; //number of correct guesses
    private int guessRows; //number of rows displaying
    private SecureRandom random; //usado pra randomizar os nomes
    private Handler handler; // usado pra atrasar um pouco o carregamento da bandeira
    private Animation shakeAnimation; //pra fazer a animação

    private LinearLayout quizLinearLayout; //o layout com o quiz
    private TextView questionNumberTextView; //mostra em qual questao esta
    private ImageView flagImageView; //mostra a bandeira
    private LinearLayout[] guessLinearLayouts; //as linhas de botoes
    private TextView answerTextView; // mostra a resposta correta


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
// esta linha abaixo tem no livro mas nao sei se precisa
// super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();

        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3);

        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0]= (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1]= (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2]= (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3]= (LinearLayout) view.findViewById(R.id.row4LinearLayout);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);

        //configura os listeners para os botoes
        for(LinearLayout row : guessLinearLayouts){
            for(int column = 0; column < row.getChildCount(); column++){
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));
        return view;
    }

    public void updateGuessRows(SharedPreferences sharedPreferences){
        //pega o numero das preferencias
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);

        guessRows = Integer.parseInt(choices)/2;

        //esconde todos os botoes
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        //mostra os que devem aparecer agora
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
    }

    public void updateRegions(SharedPreferences sharedPreferences){
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    public void resetQuiz(){
        //usa assetManager pra pegar as imagens da pasta assets
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();//esvazia a lista de nomes pra definir a nova com preferencias

        try{
            for (String region : regionsSet){
                //pega a lista das imagens de cada região
                String[] paths = assets.list(region);

                for (String path : paths){
                    fileNameList.add(path.replace(".png", ""));
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Error loading image file names", e);
        }

        //resetar valores
        correctAnswers = 0;
        totalGuesses = 0;
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //adiciona arquivos random na lista quizCountryList
        while (flagCounter <= FLAGS_IN_QUIZ){
            int randomIndex = random.nextInt(numberOfFlags);

            //pega o nome do arquivo
            String filename = fileNameList.get(randomIndex);

            //verifica se já nao foi escolhida e adiciona na lista
            if(!quizCountriesList.contains(filename)){
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }
        loadNextFlag(); //começa o quiz carregando a primeira bandeira
    }

    //quando o user acerta a bandeira carrega a proxima bandeira
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void loadNextFlag(){
        //pega o nome da proxima bandeira e remove da lista
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage; //pega a resposta correta
        answerTextView.setText(""); //limpa a resposta

        //mostra qual questao esta
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));

        //pega a regiao da proxima imagem, o formato do nome é regionName-countryName
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //usa AssetManager pra carregar imagens
        AssetManager assets = getActivity().getAssets();

        //pega um InputStream da representacao do asset da proxima bandeira e carrega como drawable

        try (InputStream stream = assets.open(region + "/" + nextImage + ".png")){
            //coloca a bandeira no drawable
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);

            animate(false); //anima a bandeira na tela
        } catch (IOException e) {
            Log.e(TAG, "Error loading " + nextImage, e);
        }

        Collections.shuffle(fileNameList); //embaralha os nomes

        //coloca a resposta correta no fim da lista
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //adiciona os botoes
        for (int row = 0; row < guessRows; row ++){
            //posiciona na tabela correta
            for (int column = 0; column < guessLinearLayouts[row].getChildCount();column++){
                //pega a referencia pra configurar
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);

                //pega o nome do pais e coloca no botao
                String filename = fileNameList.get((row*2)+column);
                newGuessButton.setText(getCountryName(filename));
            }
        }

        //substitui um dos botoes com a resposta correta randomicamente
        int row = random.nextInt(guessRows); //pega uma linha aleatoria
        int column = random.nextInt(2); //pega coluna aleatoria
        LinearLayout randomRow = guessLinearLayouts[row];
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }

    //metodo faz parse do nome do arquivo e passa o nome do pais
    private String getCountryName(String name){
        return name.substring(name.indexOf('-') + 1).replace('-', ' ');
    }

    //pra animar o quiz
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void animate(boolean animateOut){
        //previne a animacao pra primeira bandeira
        if(correctAnswers == 0)
            return;

        //calcula o centro x e y
        int centerX = (quizLinearLayout.getLeft()+ quizLinearLayout.getRight())/2;
        int centerY = (quizLinearLayout.getTop()+ quizLinearLayout.getBottom())/2;

        //calcula o raio
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        if(animateOut){
            //cria o reveal circular

                animator = ViewAnimationUtils.createCircularReveal(
                        quizLinearLayout, centerX, centerY, radius, 0);

            animator.addListener(
                    new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animation) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            loadNextFlag();
                        }

                        @Override
                        public void onAnimationCancel(Animator animation) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animation) {

                        }

                    }
            );
        }
        else {
            animator = ViewAnimationUtils.createCircularReveal(
                    quizLinearLayout, centerX, centerY, 0, radius);
        }

        animator.setDuration(500);
        animator.start();
    }

    private View.OnClickListener guessButtonListener = new View.OnClickListener() {
        @TargetApi(Build.VERSION_CODES.M)
        @Override
        public void onClick(View v) {
            Button guessButton = (Button) v;
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGuesses;

            if(guess.equals(answer)){
                ++correctAnswers;

                //mostra a resposta em verde
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer,getContext().getTheme()));

                disableButtons();

                //se o user já jogou as 10x FLAGS_IN_QUIZ
                if(correctAnswers == FLAGS_IN_QUIZ) {
                    //este é independente do livro, testei e funcionou
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(getString(R.string.results, totalGuesses,
                                    (1000 / (double) totalGuesses)));
                    //reset quiz button
                    builder.setPositiveButton(R.string.reset_quiz,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    resetQuiz();
                                }
                            });
                    builder.setCancelable(false);
                    builder.show();
                }
                else {
                    //a resposta esta correta mas o jogo nao acabou ainda
                    //carrega a proxima 2 seg depois
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    animate(true);
                                }
                            }, 2000);
                }
            }
            else{
                //resposta incorreta
                flagImageView.startAnimation(shakeAnimation);

                //mostra "incorrect" em vermelho
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false);
            }
        }
    };

    private void disableButtons(){
        for (int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for(int i = 0; i < guessRow.getChildCount();i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }
}



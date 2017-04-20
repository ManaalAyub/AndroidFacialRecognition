package com.lordjoe.identifier.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.lordjoe.identifier.FaceRecognizerType;
import com.lordjoe.identifier.IdentificationResult;
import com.lordjoe.identifier.OpenCVUtilities;
import com.lordjoe.identifier.R;
import com.lordjoe.identifier.RegisteredPerson;
import com.lordjoe.identifier.RegisteredPersonSet;
import com.lordjoe.identifier.threads.DefaultExecutorSupplier;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.lordjoe.identifier.OpenCVUtilities.cleanDirectory;
import static com.lordjoe.identifier.OpenCVUtilities.getRecognitionRoot;
import static com.lordjoe.identifier.OpenCVUtilities.loadFaceDetector;
import static com.lordjoe.identifier.RegisteredPerson.exemplarFromDir;
import static com.lordjoe.identifier.RegisteredPersonSet.NUMBER_TEST_IMAGES;

public class MainActivity extends AppCompatActivity {
    private static MainActivity activeInstance;
    private static Random RND = new Random();

    public static MainActivity getActiveInstance() {
        return activeInstance;
    }

    public static File getTempDir(String name) {
        File tmpDir = new File(getRecognitionRoot(), name);
        tmpDir.mkdirs();
        return tmpDir;
    }

    public static void setActiveInstance(MainActivity activeInstance) {
        MainActivity.activeInstance = activeInstance;
    }

    private RegisteredPersonSet registeredPeople;
 //   private Spinner peopleSelect;
    private ImageView unknown;
    private ImageView recognized;
    private TextView unknownResult;
 //   private Button recordButton;
    private Button buildRecognizerButton;
    private Button identifyButton;
    private Button identifyAndTrainButton;
  //  private Button showPersonButton;
    private PersonSelectorAdapter adapter;

    private View rootView;
    private DefaultExecutorSupplier runner;

    public RegisteredPersonSet getRegisteredPeople() {
        return registeredPeople;
    }

    public PersonSelectorAdapter getAdapter() {
        return adapter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActiveInstance(this);
        OpenCVUtilities.setAppContext(this
        );
        setContentView(R.layout.activity_main);
        unknownResult = (TextView) findViewById(R.id.identificationResult);
        unknown = (ImageView) findViewById(R.id.unknown);
        recognized = (ImageView) findViewById(R.id.recognized);
        //      recordButton = (Button) findViewById(R.id.btnRecord);
  //      recordButton = (Button) findViewById(R.id.btnRecord);
  //      recordButton.setOnClickListener(new View.OnClickListener() {
//
    //        @Override
  //          public void onClick(View v) {
  //              onRecord();
  //          }
  //      });
        buildRecognizerButton = (Button) findViewById(R.id.btnBuildRecognizer);
        buildRecognizerButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onRecognizer();
            }
        });
        identifyButton = (Button) findViewById(R.id.btnIdentifyUnknown);
        identifyButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onIdentifyUnknown();
            }
        });
        identifyAndTrainButton = (Button) findViewById(R.id.btnIdentifyAndTrainUnknown);
        identifyAndTrainButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onIdentifyAndTrainUnknown();
            }
        });
//        showPersonButton = (Button) findViewById(R.id.btnIdentify);
//        showPersonButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                onSelectPerson();
//            }
//        });
//        peopleSelect = (Spinner) findViewById(R.id.spinner);
        registeredPeople = null;
        RegisteredPerson[] objects = {};
        adapter = new PersonSelectorAdapter(this, R.layout.spinner_value_layout, objects);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // see http://stackoverflow.com/questions/5999262/populate-spinner-dynamically-in-android-from-edit-text
//        peopleSelect.setAdapter(adapter);
//
        rootView = identifyAndTrainButton.getRootView();
        runner = DefaultExecutorSupplier.getInstance();
        setDone();
    }

    private void onIdentifyUnknown() {
        identifyButton.setEnabled(false);
        identifyAndTrainButton.setEnabled(false);
        runInBackground(new Runnable() {
            @Override
            public void run() {

                setWorking();
                try {
                    if (registeredPeople == null || registeredPeople.size() == 0)
                        return;
                    cleanDirectory(getTempDir("tmp"));
                    cleanDirectory(getTempDir("tmpImages"));
                    Log.e("onIdentify", "hit");
                    List<File> testImages = new ArrayList<File>();
                    final RegisteredPerson testPerson = getRegisteredPerson(testImages);
                    final IdentificationResult identificationResult = registeredPeople.identifyUnknown(testPerson);
                    RegisteredPerson identified = null;
                    if(identificationResult != null)
                        identified = registeredPeople.getById(identificationResult.label);

                    final Bitmap image = AndroidUtilities.fromFile(testPerson.getExemplar(),120,120);
                    final Bitmap identifiedImage = identified == null? null :  AndroidUtilities.fromFile(identified.getExemplar(),120,120);;


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            unknown.setImageBitmap(image);
                            if(identificationResult == null)
                                unknownResult.setText("Unidentified");
                            else {
                                RegisteredPerson byId = registeredPeople.getById(identificationResult.label);
                                unknownResult.setText("Identified " + byId.getName());
                                if(identifiedImage != null)
                                    recognized.setImageBitmap(identifiedImage);
                                else
                                    recognized.setImageBitmap(AndroidUtilities.getGreenBitmap());
                            }

                        }
                    });
                } finally {
                    setDone();
                }
            }
        });
    }

    public RegisteredPerson getRegisteredPerson( List<File> testImages) {
        File unknownPerson = chooseUnRecognizedPerson(registeredPeople);
        Integer unusedId = registeredPeople.findUnusedId();
        String name = unknownPerson.getName();
        File exemplar = exemplarFromDir( unknownPerson);

        File tmpDir = getTempDir("tmp");

        return RegisteredPerson.createRegisteredPersonAndTestImages(registeredPeople,unknownPerson,testImages,NUMBER_TEST_IMAGES,tmpDir);
    }

    public File chooseUnRecognizedPerson(RegisteredPersonSet registered)
    {
        File file = new File(getRecognitionRoot(), "unknownPeople");
        File[] unknownPeople = file.listFiles();
        if (unknownPeople == null )
            return null;
        File unknownPerson = null;
        while(unknownPerson == null || unknownPerson.listFiles() == null) {
            unknownPerson =unknownPeople[RND.nextInt(unknownPeople.length)];
            String name = unknownPerson.getName();
            if(registered.getByName(name) == null)
                break;
        }
        return unknownPerson;
    }
    private void onIdentifyAndTrainUnknown() {
        identifyButton.setEnabled(false);
        identifyAndTrainButton.setEnabled(false);
        runInBackground(new Runnable() {
            @Override
            public void run() {

                setWorking();
                try {
                    if (registeredPeople == null || registeredPeople.size() == 0)
                        return;
                    cleanDirectory(getTempDir("tmp"));
                    cleanDirectory(getTempDir("tmpImages"));
                    Log.e("TrainUnknown", "hit");
                    List<File> testImages = new ArrayList<File>();
                    final RegisteredPerson testPerson = getRegisteredPerson(testImages);
                    final IdentificationResult original = registeredPeople.identifyUnknown(testPerson);
                    IdentificationResult identify  = null;
                    final Bitmap image = AndroidUtilities.fromFile(testPerson.getExemplar(),120,120);
                    if(original == null) { // train and try again
                        registeredPeople.update(testPerson);

                        File tmpDir = getTempDir("tmpImages");
                        List<File> cropped = OpenCVUtilities.makeCroppedImages(testImages, tmpDir);
                         identify = registeredPeople.identify(cropped);
                    }
                    final IdentificationResult finalResult = identify;
                    final Bitmap afterTraining = finalResult != null ? image : AndroidUtilities.getGreenBitmap();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            unknown.setImageBitmap(image);
                            recognized.setImageBitmap(afterTraining);
                            if(original != null) {
                                  RegisteredPerson byId = registeredPeople.getById(original.label);
                                unknownResult.setText("Falsly Identified " + byId.getName());

                            }
                            else {
                                if(finalResult != null) {
                                    unknownResult.setText("Trained " + testPerson.getName() + " " + (int)finalResult.confidence);
                                  }
                               else {
                                    unknownResult.setText("Failed Train");
                                    recognized.setImageBitmap(AndroidUtilities.getGreenBitmap());
                                }
                            }


                        }
                    });
                } finally {
                    setDone();
                }
            }
        });
    }

    private void runInBackground(Runnable run) {
        runner.forBackgroundTasks().execute(run);
    }

    public void onSelectPerson() {
        if (registeredPeople == null)
            return;
        Intent intent = new Intent(MainActivity.this, RegisteredPersonViewActivity.class);
        startActivity(intent);
        Log.e("onSelectPerson", "hit");

    }


    public void onRecord() {
        startActivity(new Intent(MainActivity.this, RecordActivity.class));

    }

    public void setWorking() {
        runOnUiThread(new Runnable() {
            public void run() {
                rootView.setBackgroundColor(Color.RED);

            }
        });


    }

    public void setDone() {
        runOnUiThread(new Runnable() {
            public void run() {
                  rootView.setBackgroundColor(Color.GREEN);
                boolean ready = registeredPeople != null && registeredPeople.size() > 0;
       //         recordButton.setEnabled(ready);
                identifyButton.setEnabled(ready);
      //          showPersonButton.setEnabled(ready);
                identifyAndTrainButton.setEnabled(ready);
                buildRecognizerButton.setEnabled(!ready);
            }
        });


    }

    public void onRecognizer() {
        identifyButton.setEnabled(false);
        identifyAndTrainButton.setEnabled(false);
        runInBackground(new Runnable() {
            @Override
            public void run() {

                loadFaceDetector();
                Log.e("onRecognizer", "hit");
                File file = new File(getRecognitionRoot(), "recognizedPeople");
                String path = file.getAbsolutePath();

                registeredPeople = new RegisteredPersonSet(file, FaceRecognizerType.LBPHFaceFaces);
                File storeDirectory = registeredPeople.getStoreDirectory();
                boolean exists = storeDirectory.exists();
                String eStr = exists ? "exists" : "not there";
                Log.e("onRecognizer", storeDirectory.getAbsolutePath() + " " + eStr);


                File haarFile = new File(getRecognitionRoot(), OpenCVUtilities.HAAR_CLASSIFIER_RESOURCE);
                OpenCVUtilities.loadCascade(haarFile); // force face classifier to load
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter = new PersonSelectorAdapter(MainActivity.this, R.layout.spinner_value_layout, registeredPeople.getPeople());
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        // see http://stackoverflow.com/questions/5999262/populate-spinner-dynamically-in-android-from-edit-text
                        //peopleSelect.setAdapter(adapter);
                     }
                });
                setDone();

            }
        });
        setWorking();
    }

    public void onTestFileSystem() {
        setWorking();
        File recognitionRoot = getRecognitionRoot();
        recognitionRoot.mkdirs();
        OpenCVUtilities.writeTextFile(recognitionRoot, "Test.txt", "Mary Had A Little Lamb");
        Log.e("Recognotion Root", recognitionRoot.getAbsolutePath());

        setDone();

    }

    public void onIdentify() {
        if (registeredPeople == null || registeredPeople.size() == 0)
            return;
        startActivity(new Intent(MainActivity.this, RegisteredPersonViewActivity.class));
    }
}

package com.example.oscar.cameratest;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.oscar.CameraHelper.CameraPreview;
import com.example.oscar.CameraHelper.CameraSaveFile;
import com.example.oscar.DrawHelper.RectangleDrawer;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static android.R.attr.left;
import static android.R.attr.right;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> words = new ArrayList<>();
    private ArrayList<int[]> bboxes = new ArrayList<>();
    private ArrayList<int[]> bboxesToDraw = new ArrayList<>();
    private Context context;
    private Button searchButton;
    private Button captureButton;
    private EditText editView;
    private FrameLayout preview;
    private RectangleDrawer rect;
    private static final String TAG = "MyCameraApp";
    private Camera mCamera = null;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isTakingPicture;
    private String datapath;
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            //new CameraSaveFile().execute(data);
            new TesseractHelper().execute(data, editView.getText().toString());
            isTakingPicture = false;
            mCamera.startPreview();

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            mCamera = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
        }

        datapath = getFilesDir() + "/tesseract/";
        checkFile(new File(datapath + "tessdata/"));

        init();
    }


    //hacer setlayoutparams con parametros que soporta camara, y hacerlos igual a picture size


    //inicializa botones, views y camara
    private void init()
    {
        // Create our Preview view and set it as the content of our activity.
        mPreview = new CameraPreview(this, mCamera);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
        //para dibujar los rectangulos en las palabras
        rect = (RectangleDrawer) findViewById(R.id.rdRect);

        searchButton = (Button) findViewById(R.id.btnSearch);
        captureButton = (Button) findViewById(R.id.btnWrite);
        editView = (EditText) findViewById(R.id.etWrite);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureButton.setVisibility(View.VISIBLE);
                editView.setVisibility(View.VISIBLE);
                searchButton.setVisibility(View.INVISIBLE);
            }
        });

        captureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                //ver si el usuario escribió una palabra
                if(editView.getText().toString().matches(""))
                {
                    searchButton.setVisibility(View.VISIBLE);
                    captureButton.setVisibility(View.INVISIBLE);
                    editView.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), "Debe introducir una palabra", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    searchButton.setVisibility(View.VISIBLE);
                    captureButton.setVisibility(View.INVISIBLE);
                    editView.setVisibility(View.INVISIBLE);
                    Toast.makeText(getApplicationContext(), "Buscó la palabra: " +
                            editView.getText().toString(), Toast.LENGTH_SHORT).show();
                    if(!isTakingPicture)
                    {
                        // get an image from the camera
                        mCamera.takePicture(null, null, mPicture);
                        isTakingPicture = true;
                    }
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder(){
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (mCamera != null){
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void copyFiles() {
        try {
            //location we want the file to be at
            String filepath = datapath + "/tessdata/eng.traineddata";
            Log.i("CAMERATEST: copyFiles", "Datapath: "+datapath);

            //get access to AssetManager
            AssetManager assetManager = getAssets();

            //open byte streams for reading/writing
            InputStream instream = assetManager.open("tessdata/eng.traineddata");
            OutputStream outstream = new FileOutputStream(filepath);

            //copy the file to the location specified by filepath
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();
            Log.i("CAMERATEST: copyFiles", "archivo creado");


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void checkFile(File dir) {
        Log.i("CAMERATEST: checkFile", "adentro de checkfile");
        //directory does not exist, but we can successfully create it
        if (!dir.exists() && dir.mkdirs()){
            Log.i("CAMERATEST: checkFile", "Directorio no existe, pero se crea");
            copyFiles();
        }
        //The directory exists, but there is no data file in it
        if(dir.exists()) {
            Log.i("CAMERATEST: checkFile", "Directorio existe");
            String datafilepath = datapath + "/tessdata/eng.traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                Log.i("CAMERATEST: checkFile", "Directorio existe, pero no esta el archivo");
                copyFiles();
            }
        }
    }


    //la camara saca las fotos de width = 2560 y height = 1920 pixeles
    public void draw()
    {

        //sacar razón entre 2560/camera.width
        //y 1920/camera.height
        //y dividir boundingbox por esa razón

        //layout in the activity that the cameraView will placed in
        int layoutWidth = preview.getWidth();
        int layoutHeight = preview.getHeight();

        //razón del ancho
        double reasonWidth = (double) 2560 / layoutWidth;
        //razón del largo
        double reasonHeight = (double) 1920 / layoutHeight;


        for (int[] bb: bboxesToDraw)
        {
            //left y right
            bb[0] = (int) (bb[0] / reasonWidth);
            bb[2] = (int) (bb[2] / reasonWidth);

            //top y bottom
            bb[1] = (int) (bb[1] / reasonHeight);
            bb[3] = (int) (bb[3] / reasonHeight);
            Log.i("CAMERATEST: draw", "left top right bottom: " + bb[0] + " " + bb[1] + " "+ bb[2] + " " + bb[3]);
        }

        //left, top, right, bottom
        rect.setParameters(bboxesToDraw);
        rect.bringToFront();
    }

    public class TesseractHelper extends AsyncTask<Object, String, int[]> {

        private byte[] imageByte;
        private Bitmap image;
        private String palabra;
        private TessBaseAPI mTess = null;
        private static final String LANG = "eng";

        //params[0] = imagen en bytes[]
        //params[1] = string con palabra a buscar
        @Override
        protected int[] doInBackground(Object... params) {

            imageByte = (byte[]) params[0];
            image = BitmapFactory.decodeByteArray(imageByte, 0, imageByte.length);
            palabra = (String) params[1];

            if(mTess == null)
            {
                mTess = new TessBaseAPI();
                mTess.init(datapath, LANG);
            }

            String OCRresult = null;
            String HOCRresult = null;
            mTess.setImage(image);

            //OCRresult = mTess.getUTF8Text();
            HOCRresult = mTess.getHOCRText(0);
            ResultIterator iterator = mTess.getResultIterator();

            words.clear();
            bboxes.clear();
            bboxesToDraw.clear();


            //recorrer la lista de alabras del texto reconocido
            iterator.begin();
            words.add(iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD));
            Log.i("CAMERATEST: iterator", "word: " + iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD));
            bboxes.add(iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_WORD));

            if(palabra.equalsIgnoreCase(words.get(words.size() - 1)))
            {
                bboxesToDraw.add(iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_WORD));
            }

            while(iterator.next(TessBaseAPI.PageIteratorLevel.RIL_WORD))
            {
                words.add(iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD));
                bboxes.add(iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_WORD));
                Log.i("CAMERATEST: iterator", "word: " + iterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_WORD));

                if(palabra.equalsIgnoreCase(words.get(words.size() - 1)))
                {
                    bboxesToDraw.add(iterator.getBoundingBox(TessBaseAPI.PageIteratorLevel.RIL_WORD));
                }

            }

            for (int[] i: bboxes
                 ) {
                Log.i("CAMERATEST: iterator", "bbox: " + i[0] + " " + i[1] + " "
                        + i[2] + " "+ i[3]);
            }

            Log.i("CAMERATEST: iterator", "words-bbox: " + words.size() + " " + bboxes.size());

            return bboxes.get(0);
        }

        @Override
        protected void onPostExecute(int[] s) {
            super.onPostExecute(s);
            Log.i("CAMERATEST: postExecute", "framelayout width height: " + preview.getWidth() + " " + preview.getHeight());
            Log.i("CAMERATEST: postExecute", "razon de width: " + (double)(2560 / preview.getWidth() ));
            if(bboxesToDraw.isEmpty())
                Toast.makeText(getApplicationContext(), "No se encontró la palabra buscada", Toast.LENGTH_SHORT).show();
            else
                draw();
        }
    }
}

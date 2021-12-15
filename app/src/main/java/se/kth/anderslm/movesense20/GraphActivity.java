package se.kth.anderslm.movesense20;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.Serializable;

import se.kth.anderslm.movesense20.R;
import se.kth.anderslm.movesense20.serialization.DeserializeFromFile;
import se.kth.anderslm.movesense20.uiutils.BtDeviceAdapter;

public class GraphActivity extends AppCompatActivity implements Serializable {

    private DataPoint time;
    private DataPoint elevation;
    private LineGraphSeries<DataPoint> series;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        GraphView graphView = (GraphView) findViewById(R.id.idGraphView);
        series = new LineGraphSeries<>();

        new DeserializeFromFile(time,elevation).readFromFile(this.getFilesDir());
    }
}
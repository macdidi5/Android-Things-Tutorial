package net.macdidi5.at.thingscommanderapp;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.List;

public class DeviceAdapter extends
        RecyclerView.Adapter<DeviceAdapter.ViewHolder> {

    public static int logcounter = 0;

    private List<Device> items;
    private Context context;

    public DeviceAdapter(Context context, List<Device> items) {
        this.context = context;
        this.items = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(
                R.layout.device_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(v);
        return viewHolder;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        protected TextView name;
        protected TextView value;
        protected com.jjoe64.graphview.GraphView graph;
        protected LineGraphSeries<DataPoint> series = getSerials();

        protected View rootView;

        public ViewHolder(View view) {
            super(view);

            Log.d("" + logcounter++, "ViewHolder");

            name = (TextView) itemView.findViewById(R.id.name);
            value = (TextView) itemView.findViewById(R.id.value);
            graph = (com.jjoe64.graphview.GraphView)
                    itemView.findViewById(R.id.graph);
            Viewport vp = graph.getViewport();
            vp.setXAxisBoundsManual(true);
            vp.setMinX(0);
            vp.setMaxX(20);
            vp.setYAxisBoundsManual(true);
            vp.setMinY(0);
            vp.setMaxY(100);
            graph.addSeries(series);
            rootView = view;
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder,
                                 final int position) {
        final Device device = items.get(position);

        holder.name.setText(device.getName());
        holder.value.setText(Float.toString(device.getValue()));
        holder.graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        holder.graph.getGridLabelRenderer().setVerticalLabelsVisible(false);

        double xc = holder.graph.getSeries().get(0).getHighestValueX() + 1;
        holder.series.appendData(
                new DataPoint(xc, device.getValue()),
                true, 20);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void add(Device device) {
        boolean exist = false;

        for (Device item : items) {
            if (device.getId().equals(item.getId())) {
                exist = true;
                break;
            }
        }

        if (!exist) {
            items.add(device);
            notifyItemInserted(items.size());
        }
    }

    public void remove(int position) {
        items.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, items.size());
    }

    public void update(Device device) {
        int index = -1;

        for (int i = 0; i < items.size(); i++) {
            if (device.getId().equals(items.get(i).getId())) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            items.set(index, device);
            notifyItemChanged(index);
        }
    }

    private static LineGraphSeries<DataPoint> getSerials() {
        LineGraphSeries<DataPoint> series = new LineGraphSeries<>();
        series.setColor(Color.RED);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(6);

        for (int i = 0; i < 20; i++) {
            series.appendData(new DataPoint(i, 0), true, 20);
        }

        return series;
    }

}

package com.noahliu.ble_example.Module.Adapter;

import android.bluetooth.BluetoothGattService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.noahliu.ble_example.Module.Enitiy.ServiceInfo;
import com.noahliu.ble_example.R;

import java.util.ArrayList;
import java.util.List;

public class ExpandableListAdapter extends BaseExpandableListAdapter {
    private List<ServiceInfo> serviceInfo = new ArrayList<>();

    public OnChildClick onChildClick;

    public void setServiceInfo(List<BluetoothGattService> services){
        for (BluetoothGattService s: services) {
            this.serviceInfo.add(new ServiceInfo(s));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return serviceInfo.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return serviceInfo.get(groupPosition).getCharacteristicInfo().size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return groupPosition;
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.service_item,parent,false);
        }
        convertView.setTag(R.layout.service_item, groupPosition);
        TextView tvTitle = convertView.findViewById(R.id.textView_Title);
        TextView tvUUID = convertView.findViewById(R.id.textView_UUID);
        TextView tvValue = convertView.findViewById(R.id.textView_Descriptors);
        tvTitle.setText(serviceInfo.get(groupPosition).getTitle());
        tvUUID.setText("UUID: "+serviceInfo.get(groupPosition).getUuid().toString());
        tvValue.setText("PRIMARY SERVICE");
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.characteristic_item,parent,false);
        }
        convertView.setTag(R.layout.characteristic_item, groupPosition);
        TextView tvTitle = convertView.findViewById(R.id.textView_Title);
        TextView tvUUID = convertView.findViewById(R.id.textView_UUID);
        TextView tvProperties = convertView.findViewById(R.id.textView_Properties);
        RecyclerView recyclerView = convertView.findViewById(R.id.recyclerview_des);
        ServiceInfo.CharacteristicInfo info = serviceInfo.get(groupPosition).getCharacteristicInfo().get(childPosition);
        DescriptorAdapter adapter = new DescriptorAdapter(info.getDescriptorsInfo());
        recyclerView.setLayoutManager(new LinearLayoutManager(parent.getContext()));
        recyclerView.setAdapter(adapter);
        tvTitle.setText(info.getTitle());
        tvUUID.setText("UUID: "+info.getUuid().toString());
        tvProperties.setText("Properties: "+info.getPropertiesTags().toString());
        convertView.setOnClickListener(v -> {
            onChildClick.onChildClick(info);
        });
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }
    public interface OnChildClick{
        void onChildClick(ServiceInfo.CharacteristicInfo info);
    }
}

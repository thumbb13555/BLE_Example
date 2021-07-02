package com.noahliu.ble_example.Module.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.noahliu.ble_example.Module.Enitiy.ServiceInfo;
import com.noahliu.ble_example.R;

import java.util.List;

public class DescriptorAdapter extends RecyclerView.Adapter<DescriptorAdapter.ViewHolder> {

    List<ServiceInfo.CharacteristicInfo.DescriptorsInfo> list;

    public DescriptorAdapter(List<ServiceInfo.CharacteristicInfo.DescriptorsInfo> list) {
        this.list = list;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle,tvUuid;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.textView_Title);
            tvUuid = itemView.findViewById(R.id.textView_UUID);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.descriptor_item,parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvTitle.setText(list.get(position).getTitle());
        holder.tvUuid.setText(list.get(position).getUuid().toString());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }


}

package com.yaroslav.newfuckingtestapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class TicketsAdapter extends RecyclerView.Adapter<TicketsAdapter.MyViewHolder>{
    private List<Ticket> ticketList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView tname, tdate;

        public MyViewHolder(View view) {
            super(view);
            tname = (TextView) view.findViewById(R.id.tTitle);
            tdate = (TextView) view.findViewById(R.id.tDate);
        }
    }

    public TicketsAdapter(List<Ticket> ticketList) {
        this.ticketList = ticketList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ticket_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Ticket ticket = ticketList.get(position);
        holder.tname.setText(ticket.getmDescription());
        holder.tdate.setText(ticket.getmTime());
    }

    @Override
    public int getItemCount() {
        return ticketList.size();
    }
}













































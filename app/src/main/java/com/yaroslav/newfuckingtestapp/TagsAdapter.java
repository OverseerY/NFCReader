package com.yaroslav.newfuckingtestapp;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class TagsAdapter extends RecyclerView.Adapter<TagsAdapter.MyViewHolder>{
    private List<Tag> tagList;

    public class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mtitle, mdate;

        public MyViewHolder(View view) {
            super(view);
            mtitle = (TextView) view.findViewById(R.id.mtitle);
            mdate = (TextView) view.findViewById(R.id.mdate);
        }
    }

    public TagsAdapter(List<Tag> tagList) {
        this.tagList = tagList;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.tag_list_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Tag tag = tagList.get(position);
        holder.mtitle.setText(tag.getmTitle());
        holder.mdate.setText(tag.getmDate());
    }

    @Override
    public int getItemCount() {
        return tagList.size();
    }
}

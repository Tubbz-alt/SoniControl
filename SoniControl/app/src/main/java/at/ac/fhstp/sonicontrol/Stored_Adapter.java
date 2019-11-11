/*
 * Copyright (c) 2018, 2019. Peter Kopciak, Kevin Pirner, Alexis Ringot, Florian Taurer, Matthias Zeppelzauer.
 *
 * This file is part of SoniControl app.
 *
 *     SoniControl app is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     SoniControl app is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with SoniControl app.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.ac.fhstp.sonicontrol;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
//import android.support.annotation.NonNull;
//import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;


import java.util.ArrayList;


public class Stored_Adapter extends ArrayAdapter<String[]>{

    public Stored_Adapter(@NonNull Context context, ArrayList<String[]> resource) {
        super(context, R.layout.row_item, resource);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater storedInflater = LayoutInflater.from(getContext());

        View customView = storedInflater.inflate(R.layout.row_item,parent, false);

        String[] singleArrayItem = getItem(position);

        TextView txtLat = (TextView) customView.findViewById(R.id.latitude);
        TextView txtLon = (TextView) customView.findViewById(R.id.longitude);
        TextView txtTech = (TextView) customView.findViewById(R.id.technologyName);
        TextView txtLastDet = (TextView) customView.findViewById(R.id.lastdetection);
        TextView txtAddress = (TextView) customView.findViewById(R.id.txtAddress);
        TextView txtSpoofingStatus = (TextView) customView.findViewById(R.id.txtSpoofingStatus);
        TextView txtDetectionCounter = (TextView) customView.findViewById(R.id.txtDetectionCounter);

        txtLon.setText("Lon " + singleArrayItem[0].substring(0, 7));
        txtLat.setText("Lat " + singleArrayItem[1].substring(0, 7));
        txtTech.setText(singleArrayItem[2]);
        String formattedDate = singleArrayItem[3];
        formattedDate = formattedDate.replace("T"," ");
        formattedDate = formattedDate.replace("Z","");
        formattedDate = formattedDate.substring(0, 19);

        txtLastDet.setText(formattedDate);
        if(singleArrayItem[5].equals("Unknown")){
            String unknownAddress = getContext().getString(R.string.stored_adapter_unknown_address);
            txtAddress.setText(unknownAddress + " (Lat "+singleArrayItem[1].substring(0, 7)+", Lon "+ singleArrayItem[0].substring(0, 7)+")");
            txtLat.setVisibility(View.GONE);
            txtLon.setVisibility(View.GONE);
        }else{
            txtAddress.setText(singleArrayItem[5]);
        }
        txtDetectionCounter.setText(getContext().getString(R.string.stored_adapter_detection_counter_title)+singleArrayItem[8]);

        String spoofingStatus = null;
        if(Integer.valueOf(singleArrayItem[4])==1){
            spoofingStatus = getContext().getString(R.string.stored_detections_will_be_blocked);
            customView.setBackgroundColor(0xFFFF0015);
            //customView.setBackgroundColor(0xFFE39B26);
        }else if(Integer.valueOf(singleArrayItem[4])==0){
            spoofingStatus = getContext().getString(R.string.stored_detections_will_be_ignored);
            customView.setBackgroundColor(0xFF00C200);
            //customView.setBackgroundColor(0x00ffffff);
        }else if(Integer.valueOf(singleArrayItem[4])==4){
            spoofingStatus = getContext().getString(R.string.stored_detections_ask_again);
            customView.setBackgroundColor(0xFFE39B26);
        }
        txtSpoofingStatus.setText(spoofingStatus);

        return customView;
    }
}

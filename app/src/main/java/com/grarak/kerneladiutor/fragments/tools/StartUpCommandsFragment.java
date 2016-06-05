/*
 * Copyright (C) 2015 Willi Ye
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.grarak.kerneladiutor.fragments.tools;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import com.grarak.kerneladiutor.R;
import com.grarak.kerneladiutor.elements.DDivider;
import com.grarak.kerneladiutor.elements.cards.CardViewItem;
import com.grarak.kerneladiutor.fragments.RecyclerViewFragment;
import com.grarak.kerneladiutor.fragments.kernel.BatteryFragment;
import com.grarak.kerneladiutor.fragments.kernel.CPUFragment;
import com.grarak.kerneladiutor.fragments.kernel.CPUHotplugFragment;
import com.grarak.kerneladiutor.fragments.kernel.CPUVoltageFragment;
import com.grarak.kerneladiutor.fragments.kernel.EntropyFragment;
import com.grarak.kerneladiutor.fragments.kernel.GPUFragment;
import com.grarak.kerneladiutor.fragments.kernel.IOFragment;
import com.grarak.kerneladiutor.fragments.kernel.KSMFragment;
import com.grarak.kerneladiutor.fragments.kernel.LMKFragment;
import com.grarak.kerneladiutor.fragments.kernel.MiscFragment;
import com.grarak.kerneladiutor.fragments.kernel.ScreenFragment;
import com.grarak.kerneladiutor.fragments.kernel.SoundFragment;
import com.grarak.kerneladiutor.fragments.kernel.ThermalFragment;
import com.grarak.kerneladiutor.fragments.kernel.VMFragment;
import com.grarak.kerneladiutor.fragments.kernel.WakeFragment;
import com.grarak.kerneladiutor.fragments.kernel.WakeLockFragment;
import com.grarak.kerneladiutor.utils.Utils;
import com.grarak.kerneladiutor.utils.database.CommandDB;
import com.grarak.kerneladiutor.utils.root.Control;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by willi on 25.04.15.
 */
public class StartUpCommandsFragment extends RecyclerViewFragment implements CardViewItem.DCardView.OnDCardListener {

    private CardViewItem.DCardView mStartUpCommandsDelete;
    private CardViewItem.DCardView[] mStartUpCommands;

    @Override
    public boolean showApplyOnBoot() {
        return false;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        listcommands();
    }

    public void listcommands () {
        CommandDB commandDB = new CommandDB(getActivity());
        List<CommandDB.CommandItem> commandItems = commandDB.getAllCommands();
        final List<String> applys = new ArrayList<>();
        List<String> commands = new ArrayList<>();

        Class[] classes = {BatteryFragment.class, CPUFragment.class, CPUHotplugFragment.class,
                CPUVoltageFragment.class, EntropyFragment.class, GPUFragment.class, IOFragment.class,
                KSMFragment.class, LMKFragment.class, MiscFragment.class,
                ScreenFragment.class, SoundFragment.class, ThermalFragment.class, WakeLockFragment.class,
                VMFragment.class, WakeFragment.class
        };

        for (Class mClass : classes)
            if (Utils.getBoolean(mClass.getSimpleName() + "onboot", false, getContext())) {
                applys.addAll(Utils.getApplys(mClass));
            }

        if (applys.size() > 0)
            for (CommandDB.CommandItem commandItem : commandItems)
                for (String sys : applys) {
                    String path = commandItem.getPath();
                    if ((sys.contains(path) || path.contains(sys))) {
                        String command = commandItem.getCommand();
                        if (commands.indexOf(command) < 0)
                            commands.add(command);
                    }
                }

        if (commands.size() > 0) {
            mStartUpCommandsDelete = new CardViewItem.DCardView();
            mStartUpCommandsDelete.setTitle(getString(R.string.startup_commands_delete));
            mStartUpCommandsDelete.setOnDCardListener(this);

            addView(mStartUpCommandsDelete);

            DDivider mStartUpCommandsListDividerCard = new DDivider();
            mStartUpCommandsListDividerCard.setText(getString(R.string.startup_commands_list));
            mStartUpCommandsListDividerCard.setDescription(getString(R.string.startup_commands_list_delete));
            addView(mStartUpCommandsListDividerCard);

            mStartUpCommands = new CardViewItem.DCardView[commandItems.size()];
            for (int i = 0; i < commands.size(); i++) {
                mStartUpCommands[i] = new CardViewItem.DCardView();
                mStartUpCommands[i].setDescription(commands.get(i));
                mStartUpCommands[i].setOnDCardListener(this);

                addView(mStartUpCommands[i]);
            }
        }
        else {
                mStartUpCommandsDelete = new CardViewItem.DCardView();
                mStartUpCommandsDelete.setTitle(getString(R.string.startup_commands_none));

                addView(mStartUpCommandsDelete);
        }
    }

    public void forcerefresh(Context context) {
        view.invalidate();
        getActivity().getSupportFragmentManager().beginTransaction().detach(this).attach(this).commit();
    }
    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            switch (which){
                case DialogInterface.BUTTON_POSITIVE:
                    //Yes button clicked
                    Control.deletespecificcommand(getActivity(), null, null);
                    forcerefresh(getActivity());
                    break;

                case DialogInterface.BUTTON_NEGATIVE:
                    //No button clicked
                    break;
            }
        }
    };

    public void onClick(CardViewItem.DCardView dCardView) {

        if (dCardView == mStartUpCommandsDelete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setMessage(getString(R.string.startup_commands_delete_all)).setPositiveButton(getString(R.string.startup_commands_delete_all_yes), dialogClickListener)
                    .setNegativeButton(getString(R.string.startup_commands_delete_all_no), dialogClickListener).show();
        }
        for (int i = 0; i < mStartUpCommands.length; i++)
        if (dCardView == mStartUpCommands[i]) {
            Control.deletespecificcommand(getActivity(), null, String.valueOf(mStartUpCommands[i].getDescription()));
            forcerefresh(getActivity());
        }
    }
}

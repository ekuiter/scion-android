package org.scionlab.scion;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputLayout;

import org.scionlab.scion.as.Logger;
import org.scionlab.scion.as.ScionAS;

import java.util.Map;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

public class ScionControlFragment extends Fragment {
    private static final String SCIONLAB_CONFIGURATION_URI = MainActivity.class.getCanonicalName() + ".SCIONLAB_CONFIGURATION_URI";
    private static final String PING_ADDRESS = MainActivity.class.getCanonicalName() + ".PING_ADDRESS";
    private static final String UPDATE_USER_INTERFACE = MainActivity.class.getCanonicalName() + ".UPDATE_USER_INTERFACE";
    private static final String SCION_STATE = MainActivity.class.getCanonicalName() + ".SCION_STATE";
    private static final String COMPONENT_STATE = MainActivity.class.getCanonicalName() + ".COMPONENT_STATE";

    private SharedPreferences preferences;
    private BroadcastReceiver updateUserInterfaceReceiver;
    private MaterialButton scionButton;
    private EditText pingAddressEditText;
    private String scionLabConfigurationUri;
    private String pingAddress;
    private Chip[] chips;

    private static final int CHIP_CONTROL_INDEX = 0;
    private static final int CHIP_BORDER_ROUTER_INDEX = 1;
    private static final int CHIP_DAEMON_INDEX = 2;
    private static final int CHIP_DISPATCHER_INDEX = 3;
    private static final int CHIP_SCMP_INDEX = 4;
    private static final int CHIP_VPN_CLIENT_INDEX = 5;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView layout = (ScrollView) inflater.inflate(R.layout.fragment_main, container, false);
        FragmentActivity activity = getActivity();
        preferences = activity.getPreferences(activity.MODE_PRIVATE);
        scionButton = layout.findViewById(R.id.scionbutton);
        pingAddressEditText = layout.findViewById(R.id.pingAddressEditText);
        TextInputLayout pingAddressTextInputLayout = layout.findViewById(R.id.pingAddressTextInputLayout);
        scionLabConfigurationUri = preferences.getString(SCIONLAB_CONFIGURATION_URI, null);
        pingAddress = preferences.getString(PING_ADDRESS, getResources().getString(R.string.pingAddress));
        pingAddressEditText.setText(pingAddress);
        pingAddressTextInputLayout.setEndIconOnClickListener(view -> {
            pingAddress = pingAddressEditText.getText().toString();
            preferences.edit().putString(PING_ADDRESS, pingAddress).apply();
            ScionService.setPingAddress(pingAddress);
        });
        LogActivity.plantTree(new Logger.Tree((tag, message) -> activity.runOnUiThread(() ->
                LogActivity.append(tag, message))));


        chips = new Chip[] {
                layout.findViewById(R.id.controlServer),
                layout.findViewById(R.id.borderRouter),
                layout.findViewById(R.id.daemon),
                layout.findViewById(R.id.dispatcher),
                layout.findViewById(R.id.scmp),
                layout.findViewById(R.id.vpnClient)};

        updateUserInterface(ScionService.getState(), ScionService.getComponentState());
        return layout;
    }

    public void updateUserInterface(ScionAS.State state, Map<String, ScionAS.State> componentState) {
        if (scionButton == null) {
            // Layout hasn't been created yet!
            return;
        }

        if (state == null)
            state = ScionAS.State.STOPPED;

        if (state == ScionAS.State.STOPPED) {
            scionButton.setText(R.string.start);
            scionButton.setBackgroundColor(ContextCompat.getColor(this.getContext(), R.color.colorPrimary));
            scionButton.setOnClickListener(view ->
                    VPNPermissionFragment.askPermission(this.getActivity(), (String errorMessage) -> {
                        if (errorMessage != null) {
                            Toast.makeText(this.getActivity(), errorMessage, Toast.LENGTH_LONG).show();
                            return;
                        }
                        chooseScionLabConfiguration();
                    }));
        } else {
            scionButton.setText(R.string.stop);
            scionButton.setBackgroundColor(ContextCompat.getColor(this.getContext(),
                    state == ScionAS.State.STARTING ? R.color.colorStarting :
                            state == ScionAS.State.HEALTHY ? R.color.colorHealthy : R.color.colorUnhealthy));
            scionButton.setOnClickListener(view -> ScionService.stop(this.getContext()));
        }

        for (Chip chip : chips)
            chip.setChipIconTintResource(R.color.colorPrimary);

        componentState.forEach((k, v) -> {
            int color = v == ScionAS.State.STOPPED ? R.color.colorPrimary :
                    v == ScionAS.State.STARTING ? R.color.colorStarting :
                    v == ScionAS.State.HEALTHY ? R.color.colorHealthy : R.color.colorUnhealthy;
            if (k.equals("ControlServer"))
                chips[CHIP_CONTROL_INDEX].setChipIconTintResource(color);
            if (k.equals("BorderRouter"))
                chips[CHIP_BORDER_ROUTER_INDEX].setChipIconTintResource(color);
            if (k.equals("Daemon"))
                chips[CHIP_DAEMON_INDEX].setChipIconTintResource(color);
            if (k.equals("Dispatcher"))
                chips[CHIP_DISPATCHER_INDEX].setChipIconTintResource(color);
            if (k.equals("Scmp"))
                chips[CHIP_SCMP_INDEX].setChipIconTintResource(color);
            if (k.equals("VPNClient"))
                chips[CHIP_VPN_CLIENT_INDEX].setChipIconTintResource(color);
        });
    }

    private void chooseScionLabConfiguration() {
        Intent chooseFile;
        Intent intent;
        chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
        chooseFile.addCategory(Intent.CATEGORY_OPENABLE);
        chooseFile.setType("*/*");
        intent = Intent.createChooser(chooseFile, getString(R.string.chooseScionLabConfiguration));
        startActivityForResult(intent, 0);
        Toast.makeText(this.getContext(), R.string.chooseScionLabConfigurationToast, Toast.LENGTH_LONG).show();
    }
}

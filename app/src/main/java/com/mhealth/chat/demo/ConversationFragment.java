package com.mhealth.chat.demo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.twilio.common.AccessManager;
import com.twilio.conversations.AudioOutput;
import com.twilio.conversations.AudioTrack;
import com.twilio.conversations.CameraCapturer;
import com.twilio.conversations.CapturerErrorListener;
import com.twilio.conversations.CapturerException;
import com.twilio.conversations.Conversation;
import com.twilio.conversations.ConversationCallback;
import com.twilio.conversations.IncomingInvite;
import com.twilio.conversations.LocalMedia;
import com.twilio.conversations.LocalVideoTrack;
import com.twilio.conversations.LogLevel;
import com.twilio.conversations.MediaTrack;
import com.twilio.conversations.OutgoingInvite;
import com.twilio.conversations.Participant;
import com.twilio.conversations.TwilioConversationsClient;
import com.twilio.conversations.TwilioConversationsException;
import com.twilio.conversations.VideoRenderer;
import com.twilio.conversations.VideoScaleType;
import com.twilio.conversations.VideoTrack;
import com.twilio.conversations.VideoViewRenderer;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashSet;
import java.util.Set;

public class ConversationFragment extends Fragment {


    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = ConversationFragment.class.getName();

    /*
     * You must provide a Twilio AccessToken to connect to the Conversations service
     */
    private static final String TWILIO_ACCESS_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsImN0eSI6InR3aWxpby1mcGE7dj0xIn0.eyJqdGkiOiJTSzc4ZjY5Nzc0YjhkYzkxZGFmOTIxMTQ0NTg2NmI0MmVjLTE0NzIwMzM5ODkiLCJpc3MiOiJTSzc4ZjY5Nzc0YjhkYzkxZGFmOTIxMTQ0NTg2NmI0MmVjIiwic3ViIjoiQUM5ZjliZTFiNTU0MjI5ZmU1MGRkOGE2ZjkyOWRlMDYwNyIsImV4cCI6MTQ3MjAzNzU4OSwiZ3JhbnRzIjp7ImlkZW50aXR5IjoibWFuYWRyIiwicnRjIjp7ImNvbmZpZ3VyYXRpb25fcHJvZmlsZV9zaWQiOiJWUzM5YWQ3MjBhNGZiNTcwNzQyMzc2ZmI3OTY0NTZiODkxIn19fQ.iyDssE-cpHQtfeK0dSIsofiG7_E23xVodBXLJ8qjmbo";

    /*
     * Twilio Conversations Client allows a client to create or participate in a conversation.
     */
    private TwilioConversationsClient conversationsClient;

    /*
     * A Conversation represents communication between the client and one or more participants.
     */
    private Conversation conversation;

    /*
     * An OutgoingInvite represents an invitation to start or join a conversation with one or
     * more participants
     */
    private OutgoingInvite outgoingInvite;

    /*
     * A VideoViewRenderer receives frames from a local or remote video track and renders
     * the frames to a provided view
     */
    private VideoViewRenderer participantVideoRenderer;
    private VideoViewRenderer localVideoRenderer;

    /*
     * Android application UI elements
     */
    private FrameLayout previewFrameLayout;
    private ViewGroup localContainer;
    private ViewGroup participantContainer;
    private TextView conversationStatusTextView;
    private AccessManager accessManager;
    private CameraCapturer cameraCapturer;
    private FloatingActionButton callActionFab;
    private FloatingActionButton switchCameraActionFab;
    private FloatingActionButton localVideoActionFab;
    private FloatingActionButton muteActionFab;
    private FloatingActionButton speakerActionFab;
    private android.support.v7.app.AlertDialog alertDialog;
    private AudioManager audioManager;

    private boolean muteMicrophone;
    private boolean pauseVideo;

    private boolean wasPreviewing;
    private boolean wasLive;

    private boolean loggingOut;

    private View root;

    String identity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        root = inflater.inflate(R.layout.activity_conversation, container, false);
        BasicIPMessagingClient basicClient = TwilioApplication.get().getBasicClient();
        identity = basicClient.getIpMessagingClient().getMyUserInfo().getIdentity();
        /*
         * Load views from resources
         */
        previewFrameLayout = (FrameLayout) root.findViewById(R.id.previewFrameLayout);
        localContainer = (ViewGroup) root.findViewById(R.id.localContainer);
        participantContainer = (ViewGroup) root.findViewById(R.id.participantContainer);
        conversationStatusTextView = (TextView) root.findViewById(R.id.conversation_status_textview);

        callActionFab = (FloatingActionButton) root.findViewById(R.id.call_action_fab);
        switchCameraActionFab = (FloatingActionButton) root.findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = (FloatingActionButton) root.findViewById(R.id.local_video_action_fab);
        muteActionFab = (FloatingActionButton) root.findViewById(R.id.mute_action_fab);
        speakerActionFab = (FloatingActionButton) root.findViewById(R.id.speaker_action_fab);
        setHasOptionsMenu(true);
        EventBus.getDefault().register(this);
        return root;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        /*
         * Enable changing the volume using the up/down keys during a conversation
         */
        getActivity().setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);

        /*
         * Needed for setting/abandoning audio focus during call
         */
        audioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
        /*
         * Check camera and microphone permissions. Needed in Android M.
         */
        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            /*
             * Initialize the Twilio Conversations SDK
             */
            Log.e(TAG, "onViewCreated: Init twilio SDK");
            initializeTwilioSdk();
        }

        /*
         * Set the initial state of the UI
         */
        setCallAction();
    }


    public void doLogout() {
        /*
                 * All conversations need to be ended before tearing down the SDK
                 */
        loggingOut = true;
        if (isConversationOngoing()) {
            hangup();
        }
        logout();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == CAMERA_MIC_PERMISSION_REQUEST_CODE &&
                permissions.length > 0) {
            boolean granted = true;
            /*
             * Check if all permissions are granted
             */
            for (int i=0; i < permissions.length; i++) {
                granted = granted && (grantResults[i] == PackageManager.PERMISSION_GRANTED);
            }
            if (granted) {
                /*
                 * Initialize the Twilio Conversations SDK
                 */
                initializeTwilioSdk();
            } else {
                Toast.makeText(getActivity(), R.string.permissions_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (TwilioConversationsClient.isInitialized() &&
                conversationsClient != null &&
                !conversationsClient.isListening()) {
            conversationsClient.listen();
        }
        // Resume preview
        if(cameraCapturer != null && wasPreviewing) {
            cameraCapturer.startPreview(previewFrameLayout);
            wasPreviewing = false;
        }
        // Resume live video
        if(conversation != null && wasLive) {
            pauseVideo(false);
            wasLive = false;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (TwilioConversationsClient.isInitialized() &&
                conversationsClient != null  &&
                conversationsClient.isListening() &&
                conversation == null) {
            conversationsClient.unlisten();
        }
        // Stop preview before going to the background
        if(cameraCapturer != null && cameraCapturer.isPreviewing()) {
            cameraCapturer.stopPreview();
            wasPreviewing = true;
        }
        // Pause live video before going to the background
        if(conversation != null && !pauseVideo) {
            pauseVideo(true);
            wasLive = true;
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        TwilioConversationsClient.destroy();
    }


    /*
     * The initial state when there is no active conversation.
     */
    private void setCallAction() {
//        callActionFab.setImageDrawable(ContextCompat.getDrawable(getActivity(),
//                R.drawable.ic_call_white_24px));
//        callActionFab.show();
//        callActionFab.setOnClickListener(callActionFabClickListener());
        callActionFab.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                R.drawable.ic_call_end_white_24px));
        callActionFab.show();
        callActionFab.setOnClickListener(hangupClickListener());
        switchCameraActionFab.show();
        switchCameraActionFab.setOnClickListener(switchCameraClickListener());
        localVideoActionFab.show();
        localVideoActionFab.setOnClickListener(localVideoClickListener());
        muteActionFab.show();
        muteActionFab.setOnClickListener(muteClickListener());
        speakerActionFab.hide();
    }

    /*
     * The actions performed during hangup.
     */
    private void setHangupAction() {
        callActionFab.setImageDrawable(ContextCompat.getDrawable(getActivity(),
                R.drawable.ic_call_end_white_24px));
        callActionFab.show();
        callActionFab.setOnClickListener(hangupClickListener());
        speakerActionFab.show();
        speakerActionFab.setOnClickListener(speakerClickListener());
    }

    /*
     * Creates an outgoing conversation UI dialog
     */
    private void showCallDialog() {
        EditText participantEditText = new EditText(getActivity());
        alertDialog = createCallParticipantsDialog(participantEditText,
                callParticipantClickListener(participantEditText), cancelCallClickListener(), getActivity());
        alertDialog.show();
    }

    /*
     * Creates an incoming conversation UI dialog
     */
    private void showInviteDialog(final IncomingInvite incomingInvite) {
        alertDialog = createInviteDialog(incomingInvite.getInviter(),
                acceptCallClickListener(incomingInvite), rejectCallClickListener(incomingInvite),
                getActivity());
        alertDialog.show();
    }

    /*
     * Initialize the Twilio Conversations SDK
     */
    private void initializeTwilioSdk(){
        TwilioConversationsClient.setLogLevel(LogLevel.ERROR);

        if(!TwilioConversationsClient.isInitialized()) {
            TwilioConversationsClient.initialize(getActivity().getApplicationContext());
            /*
             * Now that the SDK is initialized we create a ConversationsClient and
             * register for incoming calls. The TwilioAccessManager manages the lifetime
             * of the access token and notifies the client of token expirations.
             */
            // OPTION 1- Generate an access token from the getting started portal https://www.twilio.com/user/account/video/getting-started
//            accessManager = new AccessManager(ConversationFragment.this,
//                    TWILIO_ACCESS_TOKEN,
//                    accessManagerListener());
//            conversationsClient =
//                    TwilioConversationsClient.create(accessManager, conversationsClientListener());
//            // Specify the audio output to use for this conversation client
//            conversationsClient.setAudioOutput(AudioOutput.SPEAKERPHONE);
//            // Initialize the camera capturer and start the camera preview
//            cameraCapturer = CameraCapturer.create(ConversationFragment.this,
//                    CameraCapturer.CameraSource.CAMERA_SOURCE_FRONT_CAMERA,
//                    capturerErrorListener());
//            startPreview();
//            // Register to receive incoming invites
//            conversationsClient.listen();

            // OPTION 2- Retrieve an access token from your own web app
            retrieveAccessTokenfromServer();
        } else {
            Log.e(TAG, "initializeTwilioSdk: TwilioConversationsClient is not Initialized" );
        }
    }


    private void startPreview() {
        cameraCapturer.startPreview(previewFrameLayout);
    }

    private void stopPreview() {
        if(cameraCapturer != null && cameraCapturer.isPreviewing()) {
            cameraCapturer.stopPreview();
        }
    }

    private void hangup() {
        if(conversation != null) {
            conversation.disconnect();
        } else if(outgoingInvite != null){
            outgoingInvite.cancel();
        }
        setAudioFocus(false);
        EventBus.getDefault().post(new HangupEvent());
    }

    private boolean isConversationOngoing() {
        return conversation != null ||
                outgoingInvite != null;
    }

    private void logout() {
        // Teardown preview
        if (cameraCapturer != null && cameraCapturer.isPreviewing()) {
            stopPreview();
            cameraCapturer = null;
        }

        conversation = null;

        // Lets unlisten first otherwise complete logout
        if (conversationsClient != null && conversationsClient.isListening()) {
            conversationsClient.unlisten();
        }
        completeLogout();
    }

    /*
     * Once all conversations have been ended and invites are no longer being listened for, the
     * Conversations SDK can be torn down
     */
    private void completeLogout() {
        conversationsClient = null;
        destroyConversationsSdk();

        // Only required if you are done using the access manager
        disposeAccessManager();

        loggingOut = false;
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        getActivity().startActivity(intent);
        getActivity().finish();
    }

    private void destroyConversationsSdk() {
        TwilioConversationsClient.destroy();
    }

    private void disposeAccessManager() {
        if (accessManager != null) {
            accessManager.dispose();
            accessManager = null;
        }
    }

    /*
     * Resets UI elements. Used after conversation has ended.
     */
    private void reset() {
        if(participantVideoRenderer != null) {
            participantVideoRenderer.release();
            participantVideoRenderer = null;
        }
        if (root == null) return;
        localContainer.removeAllViews();
        localContainer = (ViewGroup) root.findViewById(R.id.localContainer);
        participantContainer.removeAllViews();

        conversation = null;
        outgoingInvite = null;

        muteMicrophone = false;
        muteActionFab.setImageDrawable(
                ContextCompat.getDrawable(getActivity(),
                        R.drawable.ic_mic_green_24px));

        pauseVideo = false;
        localVideoActionFab.setImageDrawable(
                ContextCompat.getDrawable(getActivity(),
                        R.drawable.ic_videocam_green_24px));
        speakerActionFab.setImageDrawable(
                ContextCompat.getDrawable(getActivity(),
                        R.drawable.ic_volume_down_green_24px));
        setSpeakerphoneOn(true);

        setCallAction();
        startPreview();
    }

    private DialogInterface.OnClickListener callParticipantClickListener(final EditText participantEditText) {
        return new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*
                 * Make outgoing invite
                 */
                String participant = participantEditText.getText().toString();
                EventBus.getDefault().post(new CallEvent(participant));
            }
        };
    }

    @Subscribe
    public void doCallAction(CallEvent callEvent) {
        String participant = callEvent.getTarget();
        if (participant == null || participant.isEmpty()) return;
        Log.d(TAG, "doCallAction: participant " + participant);
        if (!participant.isEmpty() && (conversationsClient != null)) {
            stopPreview();
            // Create participants set (we support only one in this example)
            Set<String> participants = new HashSet<>();
            participants.add(participant);
            // Create local media
            LocalMedia localMedia = setupLocalMedia();
            setAudioFocus(true);

            // Create outgoing invite
            outgoingInvite = conversationsClient.inviteToConversation(participants,
                    localMedia, new ConversationCallback() {
                        @Override
                        public void onConversation(Conversation conversation,
                                                   TwilioConversationsException e) {
                            if (e == null) {
                                // Participant has accepted invite, we are in active conversation
                                ConversationFragment.this.conversation = conversation;
                                conversation.setConversationListener(conversationListener());
                            } else {
                                Log.e(TAG, e.getMessage());
                                if (!loggingOut) {
                                    hangup();
                                    reset();
                                } else {
                                    logout();
                                }
                            }
                        }
                    });
            setHangupAction();
        } else {
            Log.e(TAG, "Failed to invite participant to conversation");
            conversationStatusTextView.setText(
                    "Failed to invite participant to conversation");
        }
    }

    private DialogInterface.OnClickListener cancelCallClickListener() {
        return new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                setCallAction();
                alertDialog.dismiss();
            }
        };
    }

    private DialogInterface.OnClickListener acceptCallClickListener(
            final IncomingInvite invite) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                /*
                 * Accept incoming invite
                 */
                LocalMedia localMedia = setupLocalMedia();
                setAudioFocus(true);

                invite.accept(localMedia, new ConversationCallback() {
                    @Override
                    public void onConversation(Conversation conversation, TwilioConversationsException e) {
                        Log.e(TAG, "Accepted conversation invite");
                        if (e == null) {
                            ConversationFragment.this.conversation = conversation;
                            conversation.setConversationListener(conversationListener());
                            EventBus.getDefault().post(new CallEvent(null));
                        } else {
                            Log.e(TAG, e.getMessage());
                            hangup();
                            reset();
                        }
                    }
                });
                setHangupAction();
            }
        };
    }

    private DialogInterface.OnClickListener rejectCallClickListener(
            final IncomingInvite incomingInvite) {
        return new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                incomingInvite.reject();
                setCallAction();
            }
        };
    }

    private View.OnClickListener hangupClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hangup();
                setCallAction();
            }
        };
    }

    private View.OnClickListener switchCameraClickListener() {
        return new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(cameraCapturer != null) {
                    cameraCapturer.switchCamera();
                }
            }
        };
    }

    private View.OnClickListener localVideoClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Update pause video if it succeeds
                pauseVideo = pauseVideo(!pauseVideo) ? !pauseVideo : pauseVideo;

                if (pauseVideo) {
                    switchCameraActionFab.hide();
                    localVideoActionFab.setImageDrawable(
                            ContextCompat.getDrawable(getActivity(),
                                    R.drawable.ic_videocam_off_red_24px));
                } else {
                    switchCameraActionFab.show();
                    localVideoActionFab.setImageDrawable(
                            ContextCompat.getDrawable(getActivity(),
                                    R.drawable.ic_videocam_green_24px));
                }
            }
        };
    }

    private boolean pauseVideo(boolean pauseVideo) {
        /*
         * Enable/disable local video track
         */
        if (conversation != null) {
            LocalVideoTrack videoTrack =
                    conversation.getLocalMedia().getLocalVideoTracks().get(0);
            if(videoTrack != null) {
                return videoTrack.enable(!pauseVideo);
            }
        }
        return false;
    }

    private View.OnClickListener muteClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Mute/unmute microphone
                 */
                muteMicrophone = !muteMicrophone;
                if (conversation != null) {
                    conversation.getLocalMedia().mute(muteMicrophone);
                }
                if (muteMicrophone) {
                    muteActionFab.setImageDrawable(
                            ContextCompat.getDrawable(getActivity(),
                                    R.drawable.ic_mic_off_red_24px));
                } else {
                    muteActionFab.setImageDrawable(
                            ContextCompat.getDrawable(getActivity(),
                                    R.drawable.ic_mic_green_24px));
                }
            }
        };
    }

    private void setSpeakerphoneOn(boolean on) {
        if (conversationsClient == null) {
            Log.e(TAG, "Unable to set audio output, conversation client is null");
            return;
        }
        conversationsClient.setAudioOutput(on ? AudioOutput.SPEAKERPHONE :
                AudioOutput.HEADSET);

        if (on == true) {
            Drawable drawable = ContextCompat.getDrawable(getActivity(),
                    R.drawable.ic_volume_down_green_24px);
            speakerActionFab.setImageDrawable(drawable);
        } else {
            // route back to headset
            Drawable drawable = ContextCompat.getDrawable(getActivity(),
                    R.drawable.ic_volume_down_white_24px);
            speakerActionFab.setImageDrawable(drawable);
        }
    }

    private View.OnClickListener speakerClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                 * Audio routing to speakerphone or headset
                 */
                if (conversationsClient == null) {
                    Log.e(TAG, "Unable to set audio output, conversation client is null");
                    return;
                }
                boolean speakerOn =
                        !(conversationsClient.getAudioOutput() ==  AudioOutput.SPEAKERPHONE) ?
                                true : false;
                setSpeakerphoneOn(speakerOn);
            }
        };
    }

    private View.OnClickListener callActionFabClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCallDialog();
            }
        };
    }

    /*
     * Conversation Listener
     */
    private Conversation.Listener conversationListener() {
        return new Conversation.Listener() {
            @Override
            public void onParticipantConnected(Conversation conversation, Participant participant) {
                conversationStatusTextView.setText("onParticipantConnected " + participant.getIdentity());

                participant.setParticipantListener(participantListener());
            }

            @Override
            public void onFailedToConnectParticipant(Conversation conversation,
                                                     Participant participant,
                                                     TwilioConversationsException e) {
                Log.e(TAG, e.getMessage());
                conversationStatusTextView.setText("onFailedToConnectParticipant " +
                        participant.getIdentity());
            }

            @Override
            public void onParticipantDisconnected(Conversation conversation,
                                                  Participant participant) {
                conversationStatusTextView.setText("onParticipantDisconnected " +
                        participant.getIdentity());
            }

            @Override
            public void onConversationEnded(Conversation conversation,
                                            TwilioConversationsException e) {
                conversationStatusTextView.setText("onConversationEnded");
                // If logging out complete the process once conversation has ended
                if (loggingOut) {
                    logout();
                } else {
                    reset();
                }
            }
        };
    }

    /*
     * LocalMedia listener
     */
    private LocalMedia.Listener localMediaListener(){
        return new LocalMedia.Listener() {
            @Override
            public void onLocalVideoTrackAdded(LocalMedia localMedia,
                                               LocalVideoTrack localVideoTrack) {
                conversationStatusTextView.setText("onLocalVideoTrackAdded");
                localVideoRenderer = new VideoViewRenderer(getActivity(),
                        localContainer);
                localVideoRenderer.applyZOrder(true);
                localVideoTrack.addRenderer(localVideoRenderer);
            }

            @Override
            public void onLocalVideoTrackRemoved(LocalMedia localMedia,
                                                 LocalVideoTrack localVideoTrack) {
                conversationStatusTextView.setText("onLocalVideoTrackRemoved");
                localContainer.removeAllViews();
                localVideoRenderer.release();
            }

            @Override
            public void onLocalVideoTrackError(LocalMedia localMedia,
                                               LocalVideoTrack localVideoTrack,
                                               TwilioConversationsException e) {
                Log.e(TAG, "LocalVideoTrackError: " + e.getMessage());
            }
        };
    }

    /*
     * Participant listener
     */
    private Participant.Listener participantListener() {
        return new Participant.Listener() {
            @Override
            public void onVideoTrackAdded(Conversation conversation,
                                          Participant participant,
                                          VideoTrack videoTrack) {
                Log.i(TAG, "onVideoTrackAdded " + participant.getIdentity());
                conversationStatusTextView.setText("onVideoTrackAdded " +
                        participant.getIdentity());

                // Remote participant
                participantVideoRenderer = new VideoViewRenderer(getActivity(),
                        participantContainer);

                // Scale the remote video to fill the view group
                participantVideoRenderer.setVideoScaleType(VideoScaleType.ASPECT_FILL);

                participantVideoRenderer.setObserver(new VideoRenderer.Observer() {

                    @Override
                    public void onFirstFrame() {
                        Log.i(TAG, "Participant onFirstFrame");
                    }

                    @Override
                    public void onFrameDimensionsChanged(int width, int height, int rotation) {
                        Log.i(TAG, "Participant onFrameDimensionsChanged " + width + " " +
                                height + " " + rotation);
                    }

                });
                videoTrack.addRenderer(participantVideoRenderer);

            }

            @Override
            public void onVideoTrackRemoved(Conversation conversation,
                                            Participant participant,
                                            VideoTrack videoTrack) {
                Log.i(TAG, "onVideoTrackRemoved " + participant.getIdentity());
                conversationStatusTextView.setText("onVideoTrackRemoved " +
                        participant.getIdentity());
                participantContainer.removeAllViews();
                participantVideoRenderer.release();

            }

            @Override
            public void onAudioTrackAdded(Conversation conversation,
                                          Participant participant,
                                          AudioTrack audioTrack) {
                Log.i(TAG, "onAudioTrackAdded " + participant.getIdentity());
            }

            @Override
            public void onAudioTrackRemoved(Conversation conversation,
                                            Participant participant,
                                            AudioTrack audioTrack) {
                Log.i(TAG, "onAudioTrackRemoved " + participant.getIdentity());
            }

            @Override
            public void onTrackEnabled(Conversation conversation,
                                       Participant participant,
                                       MediaTrack mediaTrack) {
                Log.i(TAG, "onTrackEnabled " + participant.getIdentity());
            }

            @Override
            public void onTrackDisabled(Conversation conversation,
                                        Participant participant,
                                        MediaTrack mediaTrack) {
                Log.i(TAG, "onTrackDisabled " + participant.getIdentity());
            }
        };
    }

    /*
     * ConversationsClient listener
     */
    private TwilioConversationsClient.Listener conversationsClientListener() {
        return new TwilioConversationsClient.Listener() {
            @Override
            public void onStartListeningForInvites(TwilioConversationsClient conversationsClient) {
                conversationStatusTextView.setText("onStartListeningForInvites");
            }

            @Override
            public void onStopListeningForInvites(TwilioConversationsClient conversationsClient) {
                conversationStatusTextView.setText("onStopListeningForInvites");
                // If we are logging out let us finish the teardown process
                if (loggingOut) {
                    completeLogout();
                }
            }

            @Override
            public void onFailedToStartListening(TwilioConversationsClient conversationsClient,
                                                 TwilioConversationsException e) {
                conversationStatusTextView.setText("onFailedToStartListening");
            }

            @Override
            public void onIncomingInvite(TwilioConversationsClient conversationsClient,
                                         IncomingInvite incomingInvite) {
                conversationStatusTextView.setText("onIncomingInvite");
                if (conversation == null) {
                    showInviteDialog(incomingInvite);
                } else {
                    Log.w(TAG, String.format("Conversation in progress. Invite from %s ignored",
                            incomingInvite.getInviter()));
                }
            }

            @Override
            public void onIncomingInviteCancelled(TwilioConversationsClient conversationsClient,
                                                  IncomingInvite incomingInvite) {
                conversationStatusTextView.setText("onIncomingInviteCancelled");
                alertDialog.dismiss();
                Snackbar.make(conversationStatusTextView, "Invite from " +
                        incomingInvite.getInviter() + " terminated", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        };
    }

    /*
     * CameraCapture error listener
     */
    private CapturerErrorListener capturerErrorListener() {
        return new CapturerErrorListener() {
            @Override
            public void onError(CapturerException e) {
                Log.e(TAG, "Camera capturer error: " + e.getMessage());
            }
        };
    }

    /*
     * AccessManager listener
     */
    private AccessManager.Listener accessManagerListener() {
        return new AccessManager.Listener() {
            @Override
            public void onTokenExpired(AccessManager twilioAccessManager) {
                conversationStatusTextView.setText("onAccessManagerTokenExpire");

            }

            @Override
            public void onTokenUpdated(AccessManager twilioAccessManager) {
                conversationStatusTextView.setText("onTokenUpdated");

            }

            @Override
            public void onError(AccessManager twilioAccessManager, String s) {
                conversationStatusTextView.setText("onError");
            }
        };
    }


    /*
     * Helper methods
     */

    private LocalMedia setupLocalMedia() {
        LocalMedia localMedia = new LocalMedia(localMediaListener());
        LocalVideoTrack localVideoTrack = new LocalVideoTrack(cameraCapturer);
        if (pauseVideo) {
            localVideoTrack.enable(false);
        }
        localMedia.addLocalVideoTrack(localVideoTrack);
        if (muteMicrophone) {
            localMedia.mute(true);
        }
        return localMedia;
    }

    private boolean checkPermissionForCameraAndMicrophone(){
        int resultCamera = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO);
        if ((resultCamera == PackageManager.PERMISSION_GRANTED) &&
                (resultMic == PackageManager.PERMISSION_GRANTED)){
            return true;
        } else {
            return false;
        }
    }

    private void requestPermissionForCameraAndMicrophone(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.RECORD_AUDIO)){
            Toast.makeText(getActivity(),
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    getActivity(),
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
    }

    private void retrieveAccessTokenfromServer() {
        Log.e(TAG, "retrieveAccessTokenfromServer: with identity " + identity );
        Ion.with(this)
                .load(getString(R.string.api_url) + "/token.php")
                .addQuery("identity", identity)
                .asJsonObject()
                .setCallback(new FutureCallback<JsonObject>() {
                    @Override
                    public void onCompleted(final Exception e, final JsonObject result) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (e == null) {
                                    // The identity can be used to receive calls
                                    String identity = result.get("identity").getAsString();
                                    String accessToken = result.get("token").getAsString();
                                    Log.e(TAG, "Receive token "  + accessToken);
                                    accessManager = new AccessManager(getActivity(),
                                            accessToken,
                                            accessManagerListener());
                                    conversationsClient =
                                            TwilioConversationsClient
                                                    .create(accessManager,
                                                            conversationsClientListener());
                                    // Specify the audio output to use for this conversation client
                                    conversationsClient.setAudioOutput(AudioOutput.SPEAKERPHONE);
                                    // Initialize the camera capturer and start the camera preview
                                    cameraCapturer = CameraCapturer.create(getActivity(),
                                            CameraCapturer.CameraSource.CAMERA_SOURCE_FRONT_CAMERA,
                                            capturerErrorListener());
                                    startPreview();

                                    // Register to receive incoming invites
                                    conversationsClient.listen();
                                } else {
                                    Toast.makeText(getActivity(),
                                            R.string.error_retrieving_access_token, Toast.LENGTH_SHORT)
                                            .show();
                                }
                            }
                        });
                    }
                });
    }

    private int savedAudioMode = AudioManager.MODE_INVALID;
    private void setAudioFocus(boolean setFocus) {
        if (audioManager != null) {
            if (setFocus) {
                savedAudioMode = audioManager.getMode();
                // Request audio focus before making any device switch.
                audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);

                // Start by setting MODE_IN_COMMUNICATION as default audio mode. It is
                // required to be in this mode when playout and/or recording starts for
                // best possible VoIP performance.
                // Some devices have difficulties with speaker mode if this is not set.
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            } else {
                audioManager.setMode(savedAudioMode);
                audioManager.abandonAudioFocus(null);
            }
        }
    }

    public AlertDialog createInviteDialog(String caller, DialogInterface.OnClickListener acceptClickListener, DialogInterface.OnClickListener rejectClickListener, Context context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setIcon(R.drawable.ic_call_black_24dp);
        alertDialogBuilder.setTitle("Conversation invite received");
        alertDialogBuilder.setMessage(caller + " invited you to conversation");
        alertDialogBuilder.setPositiveButton("Accept", acceptClickListener);
        alertDialogBuilder.setNegativeButton("Reject", rejectClickListener);
        alertDialogBuilder.setCancelable(false);

        return alertDialogBuilder.create();
    }

    public AlertDialog createCallParticipantsDialog(EditText participantEditText, DialogInterface.OnClickListener callParticipantsClickListener, DialogInterface.OnClickListener cancelClickListener, Context context) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setIcon(R.drawable.ic_call_black_24dp);
        alertDialogBuilder.setTitle("Invite Participant");
        alertDialogBuilder.setPositiveButton("Send", callParticipantsClickListener);
        alertDialogBuilder.setNegativeButton("Cancel", cancelClickListener);
        alertDialogBuilder.setCancelable(false);

        setParticipantFieldInDialog(participantEditText, alertDialogBuilder, context);

        return alertDialogBuilder.create();
    }

    private void setParticipantFieldInDialog(EditText participantEditText, AlertDialog.Builder alertDialogBuilder, Context context) {
        // Add a participant field to the dialog
        participantEditText.setHint("participant name");
        int horizontalPadding = context.getResources().getDimensionPixelOffset(R.dimen.activity_horizontal_margin);
        int verticalPadding = context.getResources().getDimensionPixelOffset(R.dimen.activity_vertical_margin);
        alertDialogBuilder.setView(participantEditText, horizontalPadding, verticalPadding, horizontalPadding, 0);
    }
}

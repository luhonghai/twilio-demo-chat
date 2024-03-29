package com.mhealth.chat.demo;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

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

public class ConversationActivity extends BaseActivity {

    private static final Logger logger = Logger.getLogger(ConversationActivity.class);
    private static final int CAMERA_MIC_PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = ConversationActivity.class.getName();
    public static final String VIDEO_ACTION = "VIDEO_ACTION";
    public static final String ACTION_CALL = "ACTION_CALL";
    public static final String ACTION_ACCEPT_CALL = "ACTION_ACCEPT_CALL";
    public static final String TARGET_IDENTITY = "TARGET_IDENTITY";
    /*
     * Twilio Conversations Client allows a client to create or participate in a conversation.
     */


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

    String targetIdentity;

    String videoAction;

    boolean isCameraReady = false;

    boolean isTwilioClientReady = false;

    boolean isActionExecuted = false;

    private IncomingInvite invite;

    private TwilioConversationsClient conversationsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);
        conversationsClient = MainApplication.get().getBasicClient().getConversationsClient();
        isTwilioClientReady = conversationsClient.isListening();
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null && bundle.containsKey(VIDEO_ACTION)) {
            videoAction = bundle.getString(VIDEO_ACTION);
            targetIdentity = bundle.getString(TARGET_IDENTITY);
        } else {
            this.finish();
        }
        previewFrameLayout = (FrameLayout) this.findViewById(R.id.previewFrameLayout);
        localContainer = (ViewGroup) this.findViewById(R.id.localContainer);
        participantContainer = (ViewGroup) this.findViewById(R.id.participantContainer);
        callActionFab = (FloatingActionButton) this.findViewById(R.id.call_action_fab);
        switchCameraActionFab = (FloatingActionButton) this.findViewById(R.id.switch_camera_action_fab);
        localVideoActionFab = (FloatingActionButton) this.findViewById(R.id.local_video_action_fab);
        muteActionFab = (FloatingActionButton) this.findViewById(R.id.mute_action_fab);
        speakerActionFab = (FloatingActionButton) this.findViewById(R.id.speaker_action_fab);
        this.setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        if (!checkPermissionForCameraAndMicrophone()) {
            requestPermissionForCameraAndMicrophone();
        } else {
            initCamera();
        }
        setCallAction();

    }

    private boolean isCall() {
        return videoAction.equalsIgnoreCase(ACTION_CALL);
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
                initCamera();
            } else {
                Toast.makeText(this, R.string.permissions_needed, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkForCall() {
        logger.d("isCameraReady " + isCameraReady +" isTwilioClientReady " + isTwilioClientReady + " isActionExecuted " + isActionExecuted);
        if (isCameraReady && isTwilioClientReady && !isActionExecuted) {
            isActionExecuted = true;
            if (isCall()) {
                logger.d("Do call action");
                doCallAction(new CallEvent(targetIdentity));
            } else {
                logger.d("Do accept invite");
                LocalMedia localMedia = setupLocalMedia();
                setAudioFocus(true);
                invite = MainApplication.get().getIncomingInvite();
                if (invite == null) {
                    logger.e("No invitation. Try to logout");
                    logout();
                } else {
                    logger.d("Try to accept invite");
                    invite.accept(localMedia, new ConversationCallback() {

                        @Override
                        public void onConversation(Conversation conversation, TwilioConversationsException e) {
                            Log.e(TAG, "Accepted conversation invite");
                            if (e == null) {
                                ConversationActivity.this.conversation = conversation;
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
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

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
    protected Conversation getCurrentConversation() {
        return conversation;
    }

    /*
         * The initial state when there is no active conversation.
         */
    private void setCallAction() {
        callActionFab.setImageDrawable(ContextCompat.getDrawable(this,
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
        callActionFab.setImageDrawable(ContextCompat.getDrawable(this,
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
        EditText participantEditText = new EditText(this);
        alertDialog = createCallParticipantsDialog(participantEditText,
                callParticipantClickListener(participantEditText), cancelCallClickListener(), this);
        alertDialog.show();
    }


    private void initCamera() {
        cameraCapturer = CameraCapturer.create(ConversationActivity.this,
                CameraCapturer.CameraSource.CAMERA_SOURCE_FRONT_CAMERA,
                capturerErrorListener());
        isCameraReady = true;
        checkForCall();
    }

    private void hangup() {
        if(conversation != null) {
            conversation.disconnect();
            conversation = null;
        }
        if(outgoingInvite != null){
            outgoingInvite.cancel();
            outgoingInvite = null;
        }
        try {
            if (invite != null) {
                invite.reject();
                invite = null;
            }
        } catch (Exception e) {}
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
            cameraCapturer = null;
        }

        hangup();
        MainApplication.get().setIncomingInvite(null);
        completeLogout();
    }

    /*
     * Once all conversations have been ended and invites are no longer being listened for, the
     * Conversations SDK can be torn down
     */
    private void completeLogout() {
        conversationsClient = null;
        // Only required if you are done using the access manager
        loggingOut = false;
        this.finish();
    }

    /*
     * Resets UI elements. Used after conversation has ended.
     */
    private void reset() {
        if(participantVideoRenderer != null) {
            participantVideoRenderer.release();
            participantVideoRenderer = null;
        }
        localContainer.removeAllViews();
        localContainer = (ViewGroup) this.findViewById(R.id.localContainer);
        participantContainer.removeAllViews();

        conversation = null;
        outgoingInvite = null;

        muteMicrophone = false;
        muteActionFab.setImageDrawable(
                ContextCompat.getDrawable(this,
                        R.drawable.ic_mic_green_24px));

        pauseVideo = false;
        localVideoActionFab.setImageDrawable(
                ContextCompat.getDrawable(this,
                        R.drawable.ic_videocam_green_24px));
        speakerActionFab.setImageDrawable(
                ContextCompat.getDrawable(this,
                        R.drawable.ic_volume_down_green_24px));
        speakerActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.white)));
        setSpeakerphoneOn(true);

        setCallAction();
    }

    private DialogInterface.OnClickListener callParticipantClickListener(final EditText participantEditText) {
        return new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String participant = participantEditText.getText().toString();
                doCallAction(new CallEvent(participant));
            }
        };
    }

    @Subscribe
    public void doCallAction(CallEvent callEvent) {
        String participant = callEvent.getTarget();
        if (participant == null || participant.isEmpty()) return;
        Log.d(TAG, "doCallAction: participant " + participant);
        if (!participant.isEmpty() && (conversationsClient != null)) {
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
                                ConversationActivity.this.conversation = conversation;
                                conversation.setConversationListener(conversationListener());
                            } else {
                                logout();
                            }
                        }
                    });
            setHangupAction();
        } else {
            Log.e(TAG, "Failed to invite participant to conversation");
            logout();
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
                doLogout();
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
                            ContextCompat.getDrawable(ConversationActivity.this,
                                    R.drawable.ic_videocam_off_red_24px));
                } else {
                    switchCameraActionFab.show();
                    localVideoActionFab.setImageDrawable(
                            ContextCompat.getDrawable(ConversationActivity.this,
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
                            ContextCompat.getDrawable(ConversationActivity.this,
                                    R.drawable.ic_mic_off_red_24px));
                } else {
                    muteActionFab.setImageDrawable(
                            ContextCompat.getDrawable(ConversationActivity.this,
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
            Drawable drawable = ContextCompat.getDrawable(this,
                    R.drawable.ic_volume_down_green_24px);
            speakerActionFab.setImageDrawable(drawable);
            speakerActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(android.R.color.white)));
        } else {
            // route back to headset
            Drawable drawable = ContextCompat.getDrawable(this,
                    R.drawable.ic_volume_down_white_24px);
            speakerActionFab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorGreen)));
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
                participant.setParticipantListener(participantListener());
            }

            @Override
            public void onFailedToConnectParticipant(Conversation conversation,
                                                     Participant participant,
                                                     TwilioConversationsException e) {
                Log.e(TAG, e.getMessage());
            }

            @Override
            public void onParticipantDisconnected(Conversation conversation,
                                                  Participant participant) {
                logger.d("onParticipantDisconnected " +
                        participant.getIdentity());
            }

            @Override
            public void onConversationEnded(Conversation conversation,
                                            TwilioConversationsException e) {
                logger.d("onConversationEnded");
                // If logging out complete the process once conversation has ended
                logout();
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
                logger.d("onLocalVideoTrackAdded");
                localVideoRenderer = new VideoViewRenderer(ConversationActivity.this,
                        localContainer);
                localVideoRenderer.applyZOrder(true);
                localVideoTrack.addRenderer(localVideoRenderer);
            }

            @Override
            public void onLocalVideoTrackRemoved(LocalMedia localMedia,
                                                 LocalVideoTrack localVideoTrack) {
                logger.d("onLocalVideoTrackRemoved");
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


    public void onStartListeningForInvites(TwilioConversationsClient conversationsClient) {
        isTwilioClientReady = true;
        checkForCall();
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
                logger.d("onVideoTrackAdded " +
                        participant.getIdentity());

                // Remote participant
                participantVideoRenderer = new VideoViewRenderer(ConversationActivity.this,
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
                logger.d("onVideoTrackRemoved " +
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
        int resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int resultMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if ((resultCamera == PackageManager.PERMISSION_GRANTED) &&
                (resultMic == PackageManager.PERMISSION_GRANTED)){
            return true;
        } else {
            return false;
        }
    }

    private void requestPermissionForCameraAndMicrophone(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.RECORD_AUDIO)){
            Toast.makeText(this,
                    R.string.permissions_needed,
                    Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO},
                    CAMERA_MIC_PERMISSION_REQUEST_CODE);
        }
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

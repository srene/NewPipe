package io.fluentic.ubicdn.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;

import net.named_data.jndn.util.Blob;

import io.fluentic.ubicdn.R;
import io.fluentic.ubicdn.util.G;

/**
 * Created by srenevic on 27/08/17.
 */


public class VideoFragment extends Fragment {

    private static final String VIDEO_INFORMATION = "net.named_data.nfd.route_information";

    private VideoView video;

    private String videoName;

    public static VideoFragment
    newInstance(String videoEntry) {
        Bundle args = new Bundle();
        args.putByteArray(VIDEO_INFORMATION, videoEntry.getBytes());

        VideoFragment fragment = new VideoFragment();
        fragment.setArguments(args);

        return fragment;
    }


    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    @Override
    public void onPause()
    {
        super.onPause();

    }
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        videoName = new String(new Blob(getArguments().getByteArray(VIDEO_INFORMATION)).getImmutableArray());
        G.Log("Got arguments: " + getArguments());
        G.Log("Got byte array: " + getArguments().getByteArray(VIDEO_INFORMATION));
        G.Log("VideoFragment","Video name "+videoName);
        LayoutInflater.from(getActivity());
        //setContentView(R.layout.fragment_video);
      //  View v = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_video, null);


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup group, Bundle saved) {

        return inflater.inflate(R.layout.fragment_video,group,false);
    }
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        G.Log("VideoFragment","Video Start");
        super.onViewCreated(view, savedInstanceState);
       // View v = getLayoutInflater(savedInstanceState).inflate(R.layout.fragment_video, null);
        video=(VideoView) getView().findViewById(R.id.VideoView);

        G.Log("VideoFragment","Path "+getActivity().getFilesDir()+"/"+videoName+".mp4");
        video.setVideoPath(getActivity().getFilesDir()+"/"+videoName+".mp4");


        video.start();
    }

}

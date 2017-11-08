package io.fluentic.ubicdn.data;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import io.fluentic.ubicdn.util.G;

/**
 * Created by srene on 02/11/17.
 */

public class ContentAdvertisement {

    private BloomFilter bloomFilter;

    private static final String TAG = "ContentAdvertisement";
    // Empty constructor
    public ContentAdvertisement(){
         bloomFilter = BloomFilter.create(Funnels.byteArrayFunnel(), 300,0.05);
    }


    public void addElement(String str)
    {
        bloomFilter.put(str.getBytes());
    }

    public boolean checkElement(String str)
    {
        return bloomFilter.mightContain(str);

    }

    public String getFilter()
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try{
            bloomFilter.writeTo(out);
        } catch (IOException e)
        {
            G.Log(TAG,"Exception "+e);
        }

        return out.toString();
    }
    //Putting elements into the filter
    //A BigInteger representing a key of some sort
    //Testing for element in set
}

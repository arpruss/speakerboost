mv src/mobi/omegacentauri/SpeakerBoost_Donate src/mobi/omegacentauri/SpeakerBoost
for x in src/mobi/omegacentauri/SpeakerBoost/*.java AndroidManifest.xml res/layout/*.xml; do
    echo Fixing $x
    sed -i "s/omegacentauri\\.SpeakerBoost_Donate/omegacentauri.SpeakerBoost/" $x
done
sed -i "s/android:label=\"SpeakerBoost Pro\"/android:label=\"SpeakerBoost\"/" AndroidManifest.xml
#sed -i "s/android:label=\"ScreenDim $1\"/android:label=\"ScreenDim $2\"/" AndroidManifest.xml
#sed -i "s/android:label=\"ScreenDim Full\"/android:label=\"ScreenDim\"/" AndroidManifest.xml
#sed -i "s/android:name=\"ScreenDim\"/android:name=\"ScreenDim Full\"/" AndroidManifest.xml
#sed -i "s/android:name=\"ScreenDim $1\"/android:name=\"ScreenDim $2\"/" AndroidManifest.xml
#sed -i "s/android:name=\"ScreenDim Full\"/android:name=\"ScreenDim\"/" AndroidManifest.xml
#cp -r icons/$2/* res/
#rm current.$1
#touch current.$2

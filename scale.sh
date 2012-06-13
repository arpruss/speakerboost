convert icon512.png -scale 22.265625% icon114.png
cd res/drawable-hdpi
for x in *.png ; do 
  convert $x -scale 50% ../drawable-mdpi/$x
  convert $x -scale 33.3333333% ../drawable-ldpi/$x
done

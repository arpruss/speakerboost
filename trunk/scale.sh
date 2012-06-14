convert icon512.png -scale 22.265625% icon114.png
convert -background none config.svg res/drawable-hdpi/config.png
convert -background none config.svg -scale 66.666666666666% res/drawable-mdpi/config.png
convert -background none config.svg -scale 50% res/drawable-ldpi/config.png
cd res/drawable-hdpi
for x in *.png ; do
  convert $x -scale 66.6666666666666% ../drawable-mdpi/$x
  convert $x -scale 50% ../drawable-ldpi/$x
done

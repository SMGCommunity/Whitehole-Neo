pushd %CD%
ant -Dnb.internal.action.name=build jar
cd dist
rm -f README.TXT
curl -k -L https://github.com/IonicPixels/Whitehole-Objectdb/raw/main/objectdb.xml -o objectdb.xml
7z a ../Build.zip *.* -r
popd
echo Complete.
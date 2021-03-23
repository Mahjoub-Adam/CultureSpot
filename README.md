Instructions on how to use the application :


Download the CultureSpot folder and open it with Android Studio, which you have to download.


Connect Firebase with the project through Android Studio Tools.


Change the GraphDB_URL in MapFragment.java:89(file:line) to your GraphDB url found in the dashboard of the GraphDB application, which you have to download.


Put your Google API keys in MapFragment:841 google_maps_api:19(src/debug and src/release/res/values) AndroidManifest.xml:33 google-services.json:23.This key can be created in Google Cloud Console(google account is needed).


Load the rdf-ttl files to your  GraphDb database(First Setup->Repositories->Create a new Repository and then Import->RDF to load the files).


Run the code from Android Studio.


READ THESIS.pdf for more information

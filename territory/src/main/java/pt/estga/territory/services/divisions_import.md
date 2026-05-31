download portugal-latest.osm.pbf file from geofabrik: https://download.geofabrik.de/europe/portugal.html 

filter only necessary data (administrative boundaries):

osmium tags-filter portugal-latest.osm.pbf \
r/boundary=administrative \
r/admin_level=4,6,7,8 \
-o portugal-admin.osm.pbf


prerequisites:
osmium

sudo apt install osmium-tool
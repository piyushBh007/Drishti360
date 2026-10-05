from geopy.distance import geodesic

emulator = (37.421998, -122.084000)
jaipur = (26.912400, 75.787300)
jodhpur = (26.238900, 73.024300)

print(f"Jaipur: {geodesic(emulator, jaipur).meters}")
print(f"Jodhpur: {geodesic(emulator, jodhpur).meters}")

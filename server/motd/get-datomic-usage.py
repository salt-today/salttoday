import subprocess

# Test Output
# input = '''[{volume salttoday-vol /var/lib/docker/volumes/salttoday-vol/_data /root/datomic-volumes local z true } {volume dd0a2f15f85878054489949b7795b30d1c4a98edd7cbd7a3b35686293c048a20 /var/lib/docker/volumes/dd0a2f15f85878054489949b7795b30d1c4a98edd7cbd7a3b35686293c048a20/_data /data local  true } {volume e24438236ebc9f706d317b69822b684dc0c77fd6a6eb7dc5c23a63b5b7f61ea4 /var/lib/docker/volumes/e24438236ebc9f706d317b69822b684dc0c77fd6a6eb7dc5c23a63b5b7f61ea4/_data /log local  true }]'''

dockerOutput = subprocess.run(["docker", "inspect", "-f", "'{{ .Mounts }}'", "datomic"], capture_output=True)
volumes = dockerOutput.stdout.decode("utf-8").replace("[", "").replace("{", "").split("}")

dataFilePath = ""
logsFilePath = ""

for volume in volumes:
  if "/data" in volume:
    components = volume.strip().split(" ")
    dataFilePath = components[2]
  elif "/log" in volume:
    components = volume.strip().split(" ")
    logsFilePath = components[2]

duOutputData = subprocess.run(["du", "-hs", dataFilePath], capture_output=True)
dataFileSize = duOutputData.stdout.decode("utf-8").split("\t")[0]

duOutputLogs = subprocess.run(["du", "-hs", logsFilePath], capture_output=True)
logsFileSize = duOutputLogs.stdout.decode("utf-8").split("\t")[0]

print("\033[92mDatomic Storage Usage\033[0m - \033[93m%s\033[0m" % dataFileSize)
print("\033[92mDatomic Log Usage\033[0m - \033[93m%s\033[0m" % logsFileSize)

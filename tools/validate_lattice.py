from pathlib import Path
from sys import argv

directory = "./logs"
nb_hosts = int(argv[1]) if len(argv) > 0 else 0
files_to_get = [
    "proc" + ("0" if i < 10 else "") + f"{i}.output" for i in range(1, nb_hosts + 1)
]


def get_delivered_set(filename: str) -> list[set[int]]:
    with open(filename) as file:
        return [{int(e) for e in l.split(" ")} for l in file.readline()]


for f in Path(directory).iterdir():
    if f.name in files_to_get:
        print(f.name)

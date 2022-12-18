from pathlib import Path
from sys import argv, stderr
from os.path import join
from itertools import combinations
from typing import List, Set


def sets_in_file(filename: str) -> List[Set[int]]:
    with open(filename, "r") as file:
        return [
            {int(e) for e in line.strip().split(" ") if e != "" and line.strip() != ""}
            for line in file.readlines()
        ]


def get_filename_for_proc(parent_dir: str, proc_nb: int, extension: str) -> str:
    return join(
        parent_dir, "proc" + ("0" if proc_nb < 10 else "") + f"{proc_nb}." + extension
    )


def was_stopped_early(
    decided_values: List[Set[int]], expected_len: int, host: int
) -> bool:
    # we assume that if a process was stopped early, it does not have enough values in decided set
    if not was_printed[host - 1] and len(decided_values) < expected_len:
        print(f"Host {host} was stopped early")
        was_printed[host - 1] = True
    return len(decided_values) < expected_len


logs_dir = "logs"
if not Path(logs_dir).is_dir():
    print(
        "Cannot open the logs dir, you can modify it in the script's code (./logs/ directory expected)"
    )
    raise SystemExit(1)

if len(argv) != 3 or int(argv[1]) < 1 or int(argv[2]) < 1:
    print("Usage : python3 validate_lattice.py NB_HOSTS NB_AGREEMENTS")
    raise SystemExit(1)

nb_hosts = int(argv[1])
nb_agreements = int(argv[2])
output_files = [
    get_filename_for_proc(logs_dir, i, "output") for i in range(1, nb_hosts + 1)
]
config_files = [
    get_filename_for_proc(logs_dir, i, "config") for i in range(1, nb_hosts + 1)
]

proposals = [sets_in_file(f)[1:] for f in config_files]  # all process' proposals
decided = [sets_in_file(f) for f in output_files]  # all process'decided values
failed = False
was_printed = [False for _ in range(nb_hosts)]

for agreement in range(nb_agreements):
    all_proposals = set()
    for prop in proposals:
        # makes union of all proposals over all hosts for this agreement
        all_proposals = all_proposals.union(prop[agreement])
    for host, (prop, dec) in enumerate(zip(proposals, decided), start=1):
        if not (
            was_stopped_early(dec, nb_agreements, host)
            or prop[agreement].issubset(dec[agreement])
        ):
            print(
                f"Failed to verify Validity 1 for agreement : {agreement} at host : {host}"
            )
            failed = True

        if not (
            was_stopped_early(dec, nb_agreements, host)
            or dec[agreement].issubset(all_proposals)
        ):
            print(
                f"Failed to verify Validity 2 for agreement : {agreement} at host : {host}"
            )
            failed = True
    for (host1, dec1), (host2, dec2) in combinations(enumerate(decided, start=1), 2):
        if not (
            was_stopped_early(dec1, nb_agreements, host1)
            or was_stopped_early(dec2, nb_agreements, host2)
            or dec1[agreement].issubset(dec2[agreement])
            or dec2[agreement].issubset(dec1[agreement])
        ):
            print(
                f"Failed to verify Consistency for agreement : {agreement} at hosts : {host1} and {host2}"
            )
            failed = True

if not failed:
    print("Successfully validated all tests")

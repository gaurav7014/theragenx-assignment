.PHONY: build start stop test logs clean backup restore

build:
	ops/run.sh build

start:
	ops/run.sh start

stop:
	ops/run.sh stop

test:
	ops/run.sh test

logs:
	ops/run.sh logs

clean:
	ops/run.sh clean

backup:
	ops/backup.sh

restore:
	@[ -n "$(FILE)" ] || { echo "Usage: make restore FILE=<path/to/backup.json> [ARGS=--dry-run]"; exit 1; }
	ops/restore.sh $(ARGS) "$(FILE)"
